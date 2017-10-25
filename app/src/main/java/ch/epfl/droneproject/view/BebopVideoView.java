package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BebopVideoView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = BebopVideoView.class.getSimpleName();

    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private Surface mSurface;
    private MediaCodec mMediaCodec;
    private boolean mIsSurfaceCreated = false;
    private boolean mIsCodecConfigured = false;

    private Lock mReadyLock;
    private ByteBuffer mSpsBuffer;
    private ByteBuffer mPpsBuffer;


    public BebopVideoView(Context context) {
        this(context, null);
    }

    public BebopVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BebopVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mReadyLock = new ReentrantLock();
        setSurfaceTextureListener(this);
    }

    public void displayFrame(ARFrame frame) {

        mReadyLock.lock();

        if (mIsSurfaceCreated && mSpsBuffer != null) {

            if (!mIsCodecConfigured) {
                configureMediaCodec();
            }

            // Here we have either a good PFrame, or an IFrame
            int index = -1;

            try {
                index = mMediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error while dequeue input buffer");
            }
            if (index >= 0) {
                ByteBuffer b = mMediaCodec.getInputBuffer(index);

                if (b != null) {
                    b.put(frame.getByteData(), 0, frame.getDataSize());
                }
                try {
                    mMediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, 0);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error while queue input buffer");
                }
            }

            // Try to display previous frame
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outIndex;
            try {
                outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                while (outIndex >= 0) {
                    mMediaCodec.releaseOutputBuffer(outIndex, true);
                    outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                }

            } catch (IllegalStateException e) {
                Log.e(TAG, "Error while dequeue input buffer (outIndex)");
            }
        }
        mReadyLock.unlock();
    }

    public void configureDecoder(ARControllerCodec codec) {
        mReadyLock.lock();

        if (codec.getType() == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264) {
            ARControllerCodec.H264 codecH264 = codec.getAsH264();
            mSpsBuffer = ByteBuffer.wrap(codecH264.getSps().getByteData());
            mPpsBuffer = ByteBuffer.wrap(codecH264.getPps().getByteData());
        }
        if (mSpsBuffer != null) {
            configureMediaCodec();
        }
        mReadyLock.unlock();
    }

    private void configureMediaCodec() {

        try {
            if(mMediaCodec != null) {
                mMediaCodec.stop();
            }
            final MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
            format.setByteBuffer("csd-0", mSpsBuffer);
            format.setByteBuffer("csd-1", mPpsBuffer);

            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE);
            mMediaCodec.configure(format, mSurface, null, 0);
            mMediaCodec.start();

            mIsCodecConfigured = true;
        } catch (Exception e) {
            Log.e(TAG, "configureMediaCodec", e);
        }

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mSurface = new Surface(surface);
        mIsSurfaceCreated = true;
        setAlpha(1.0f);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

        if (mMediaCodec != null) {
            if (mIsCodecConfigured) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
            mIsCodecConfigured = false;
            mMediaCodec = null;
        }

        if (surface != null) surface.release();
        if (this.mSurface != null) this.mSurface.release();

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}