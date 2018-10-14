package io.synople.scan;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.*;
import io.synople.scan.model.Money;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Recognizer {
    private final String COLLECTION_ID = "bills";

    private AmazonRekognitionClient client;

    public Recognizer(Context context) {
        client = new AmazonRekognitionClient(new CognitoCachingCredentialsProvider(
                context,
                "[IDENTITY POOL ID e.g. us-east-1:1234567890asdf]",
                Regions.US_EAST_1
        ));
    }

    public List<Money> getFaces(Image image) {
        IndexFacesRequest request = new IndexFacesRequest().withImage(convertImage(image));
        request.setCollectionId(COLLECTION_ID);
        request.setDetectionAttributes(new ArrayList<String>() {{
            add("ALL");
        }});
        IndexFacesResult result = client.indexFaces(request);
        List<FaceRecord> records = result.getFaceRecords();

        List<Money> moneyList = new ArrayList<>();
        for (FaceRecord face : records) {
            String id = face.getFace().getFaceId();
            SearchFacesRequest searchFacesRequest = new SearchFacesRequest()
                    .withCollectionId("bills")
                    .withFaceId(id)
                    .withFaceMatchThreshold(70F);
            List<FaceMatch> matches = client.searchFaces(searchFacesRequest).getFaceMatches();
            while (matches.get(0).getFace().getExternalImageId() == null) {
                matches.remove(0);
            }
            String bestMatch = matches.get(0).getFace().getExternalImageId();
            face.getFace().setExternalImageId(bestMatch);
            BoundingBox box = face.getFaceDetail().getBoundingBox();

            moneyList.add(new Money((((box.getLeft() + (box.getWidth() / 2f)) * image.getWidth()) * 4f),
                    (((box.getTop() + (box.getHeight() / 2f)) * image.getHeight()) * 3f),
                    bestMatch));
        }

        return moneyList;
    }

    private static com.amazonaws.services.rekognition.model.Image convertImage(Image image) {

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);

        byte[] data = out.toByteArray();
        return new com.amazonaws.services.rekognition.model.Image().withBytes(ByteBuffer.wrap(data));
    }
}
