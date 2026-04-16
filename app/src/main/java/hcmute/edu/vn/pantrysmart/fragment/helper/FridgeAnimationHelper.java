package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Helper chứa các animation dùng chung cho FridgeFragment:
 * - Fade out + Scale down
 * - Fade in + Scale up
 */
public final class FridgeAnimationHelper {

    private FridgeAnimationHelper() {}

    /** Animation làm mờ và thu nhỏ view, sau đó chạy callback. */
    public static void animateViewOut(View view, Runnable onEnd) {
        view.animate()
                .alpha(0f).scaleX(0.95f).scaleY(0.95f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(onEnd).start();
    }

    /** Animation hiện rõ và phình to view. */
    public static void animateViewIn(View view) {
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
}
