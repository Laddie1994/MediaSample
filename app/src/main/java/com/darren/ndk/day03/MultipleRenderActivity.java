package com.darren.ndk.day03;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.darren.live.LivePush;

public class MultipleRenderActivity extends AppCompatActivity {
    private BitmapGLSurfaceView mSurfaceView;
    private LinearLayout mSurfaceContainer;

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_render);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceContainer = findViewById(R.id.surface_container);
        mSurfaceView.getRenderer().setOnRenderListener(new RenderListener() {
            @Override
            public void onTextureCreated(final int textureId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSurfaceContainer.removeAllViews();
                        for (int i = 0; i < 3; i++) {
                            MultipleGLSurfaceView multipleGLSv = new MultipleGLSurfaceView(MultipleRenderActivity.this);
                            multipleGLSv.setEglContext(mSurfaceView.getEglContext());
                            multipleGLSv.setTextureId(textureId);
                            multipleGLSv.getRenderer().setFragmentShader(getFragmentFilter(i));

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
                            if (i != 0) {
                                params.leftMargin = 20;
                            }
                            params.weight = 1;

                            mSurfaceContainer.addView(multipleGLSv, params);
                        }
                    }
                });
            }
        });
    }

    private int getFragmentFilter(int index) {
        switch (index) {
            case 0:
                return R.raw.filter_fragment_gray;
            case 1:
                return R.raw.filter_fragment_cool;
            case 2:
                return R.raw.filter_fragment_warm;
        }
        return R.raw.filter_fragment_warm;
    }

}