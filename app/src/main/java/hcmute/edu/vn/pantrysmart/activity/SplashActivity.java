package hcmute.edu.vn.pantrysmart.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.pantrysmart.MainActivity;
import hcmute.edu.vn.pantrysmart.R;

// Màn hình Splash - thiết kế tham khảo từ Figma PantrySmart
public class SplashActivity extends AppCompatActivity {

    // Thời gian hiển thị splash (3.5 giây)
    private static final int SPLASH_DURATION = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Lấy các view
        View imgSplashIcon = findViewById(R.id.imgSplashIcon);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);
        View splashBottom = findViewById(R.id.splashBottom);
        ProgressBar progressBar = findViewById(R.id.splashProgressBar);
        View decorLarge = findViewById(R.id.decorCircleLarge);
        View decorSmall = findViewById(R.id.decorCircleSmall);
        View decorBottom = findViewById(R.id.decorCircleBottom);

        // Animation 1: Vòng trang trí lớn - scale từ nhỏ đến lớn + xoay nhẹ
        AnimationSet decorLargeAnim = new AnimationSet(true);
        decorLargeAnim.setInterpolator(new DecelerateInterpolator());

        ScaleAnimation decorScale = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        decorScale.setDuration(1200);

        AlphaAnimation decorFade = new AlphaAnimation(0f, 1f);
        decorFade.setDuration(1000);

        decorLargeAnim.addAnimation(decorScale);
        decorLargeAnim.addAnimation(decorFade);
        decorLarge.startAnimation(decorLargeAnim);

        // Animation 2: Vòng trang trí nhỏ - delay 200ms
        AnimationSet decorSmallAnim = new AnimationSet(true);
        decorSmallAnim.setInterpolator(new DecelerateInterpolator());
        decorSmallAnim.setStartOffset(200);

        ScaleAnimation decorScale2 = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        decorScale2.setDuration(1000);

        AlphaAnimation decorFade2 = new AlphaAnimation(0f, 1f);
        decorFade2.setDuration(800);

        decorSmallAnim.addAnimation(decorScale2);
        decorSmallAnim.addAnimation(decorFade2);
        decorSmall.startAnimation(decorSmallAnim);

        // Animation 3: Vòng trang trí dưới
        AnimationSet decorBottomAnim = new AnimationSet(true);
        decorBottomAnim.setInterpolator(new DecelerateInterpolator());
        decorBottomAnim.setStartOffset(400);

        ScaleAnimation decorScale3 = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        decorScale3.setDuration(1000);

        AlphaAnimation decorFade3 = new AlphaAnimation(0f, 1f);
        decorFade3.setDuration(800);

        decorBottomAnim.addAnimation(decorScale3);
        decorBottomAnim.addAnimation(decorFade3);
        decorBottom.startAnimation(decorBottomAnim);

        // Animation 4: Icon - bounce vào, scale + overshoot
        AnimationSet iconAnim = new AnimationSet(true);
        iconAnim.setInterpolator(new OvershootInterpolator(1.5f));

        ScaleAnimation iconScale = new ScaleAnimation(
                0.2f, 1f, 0.2f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        iconScale.setDuration(900);

        AlphaAnimation iconFade = new AlphaAnimation(0f, 1f);
        iconFade.setDuration(600);

        RotateAnimation iconRotate = new RotateAnimation(
                -15f, 0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        iconRotate.setDuration(900);

        iconAnim.addAnimation(iconScale);
        iconAnim.addAnimation(iconFade);
        iconAnim.addAnimation(iconRotate);
        imgSplashIcon.startAnimation(iconAnim);

        // Animation 5: Tên app trượt lên + fade in (delay 500ms)
        AnimationSet nameAnim = new AnimationSet(true);
        nameAnim.setInterpolator(new DecelerateInterpolator());
        nameAnim.setStartOffset(500);

        TranslateAnimation nameSlide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0f);
        nameSlide.setDuration(600);

        AlphaAnimation nameFade = new AlphaAnimation(0f, 1f);
        nameFade.setDuration(600);

        nameAnim.addAnimation(nameSlide);
        nameAnim.addAnimation(nameFade);
        tvAppName.startAnimation(nameAnim);

        // Animation 6: Tagline fade in (delay 800ms)
        AlphaAnimation taglineFade = new AlphaAnimation(0f, 1f);
        taglineFade.setDuration(500);
        taglineFade.setStartOffset(800);
        tvTagline.startAnimation(taglineFade);

        // Animation 8: Khu vực dưới fade in (delay 1200ms)
        AlphaAnimation bottomFade = new AlphaAnimation(0f, 1f);
        bottomFade.setDuration(500);
        bottomFade.setStartOffset(1200);
        splashBottom.startAnimation(bottomFade);

        // Animation 9: Thanh loading tiến dần (delay 1000ms, 2 giây)
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(2200);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.setStartDelay(1000);
        progressAnimator.start();

        // Chuyển sang MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
