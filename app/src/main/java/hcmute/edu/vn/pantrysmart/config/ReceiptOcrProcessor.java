package hcmute.edu.vn.pantrysmart.config;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ReceiptOcrProcessor {

    public interface OcrRawCallback {
        void onSuccess(String rawText);

        void onError(String errorMessage);
    }

    public static void extractRawText(Bitmap bitmap, OcrRawCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String resultText = visionText.getText();
                    if (resultText == null || resultText.trim().isEmpty()) {
                        callback.onError("Không tìm thấy chữ nào trên ảnh.");
                    } else {
                        callback.onSuccess(resultText);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError("Lỗi thư viện ML Kit: " + e.getMessage());
                });
    }
}
