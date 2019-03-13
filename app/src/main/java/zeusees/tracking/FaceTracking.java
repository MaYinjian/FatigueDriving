package zeusees.tracking;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class FaceTracking {

    static {
        System.loadLibrary("zeuseesTracking-lib");
    }

    public native static void update(byte[] data, int height, int width, long session);

    public native static void initTracking(byte[] data, int height, int width, long session);

    public native static long createSession(String modelPath);

    public native static void releaseSession(long session);

    public native static int getTrackingNum(long session);

    public native static int[] getTrackingLandmarkByIndex(int index, long session);

    public native static int[] getTrackingLocationByIndex(int index, long session);

    public native static int getTrackingIDByIndex(int index, long session);

    private long session;
    private List<Face> faces;


    public FaceTracking(String pathModel) {
        session = createSession(pathModel);
        faces = new ArrayList<Face>();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        releaseSession(session);
    }

    public void FaceTrackingInit(byte[] data, int height, int width) {
        initTracking(data, height, width, session);
    }

    public void Update(byte[] data, int height, int width) {
        // 清除人脸数据
        faces.clear();

        update(data, height, width, session);
        int numsFace = getTrackingNum(session);
        if (numsFace == 0) {
            return;
        }
        Log.d("numsFace_tracking", numsFace + "");

        int[] faceRect = new int[4];
        float areaTmp, areaMax = 0;
        int id = 0, index = 0;
        for (int i = 0; i < numsFace; i++) {
            faceRect = getTrackingLocationByIndex(i, session);
            id = getTrackingIDByIndex(i, session);
            if (numsFace > 1) {
                areaTmp = faceRect[2] * faceRect[3];
                if (areaTmp > areaMax) {
                    areaMax = areaTmp;
                    index = i;
                }
            }
        }
        int[] landmarks = getTrackingLandmarkByIndex(index, session);
        Face face = new Face(faceRect[0], faceRect[1], faceRect[2], faceRect[3], landmarks, id);
        faces.add(face);
    }

    public List<Face> getTrackingInfo() {
        return faces;
    }

}
