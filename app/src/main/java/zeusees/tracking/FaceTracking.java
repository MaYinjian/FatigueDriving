package zeusees.tracking;

import android.util.Log;


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
    private Face face;
    private int lastID = -1;
    private String TAG = "FaceTracking";

    public FaceTracking(String pathModel) {
        session = createSession(pathModel);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        releaseSession(session);
    }

    public void FaceTrackingInit(byte[] data, int height, int width) {
        initTracking(data, height, width, session);
    }

    public void Update(byte[] data, int height, int width) {
        update(data, height, width, session);
        int numsFace = getTrackingNum(session);
        // 清除人脸数据
        if (numsFace == 0) {
            face = null;
            return;
        }
        Log.d(TAG, "numsFace_tracking: " + numsFace);

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
        face = new Face(faceRect[0], faceRect[1], faceRect[2], faceRect[3], landmarks, id);

        // 初始化各部位阈值
        if (EAR == 0 || face.ID != lastID) {
            lastID = face.ID;
            leftEAR = (dis2points(1, 12) + dis2points(34, 3) + dis2points(53, 67)) / (dis2points(94, 59) * 3);
            rightEAR = (dis2points(104, 51) + dis2points(41, 43) + dis2points(85, 47)) / (dis2points(27, 20) * 3);
            tmpEAR += (leftEAR + rightEAR) / 2;
            initTime += 1;
            if (initTime == 10) {
                EAR = tmpEAR / 10;
                initTime = 0;
            }
        } else {
            // 检测各部位疲劳状态
            if (checkEyesClosed()) {
                Log.e(TAG, "WARNING: 检测到闭眼");
            }
            if (checkYawn()) {
                Log.e(TAG, "WARNING: 检测到打哈欠");
            }
            if (checkHeadTurned()) {
                Log.e(TAG, "WARNING: 检测到左顾右盼");
            }
        }
    }

    public double dis2points(int point1, int point2) {
        double x = Math.abs(face.landmarks[2 * point1] - face.landmarks[2 * point2]);
        double y = Math.abs(face.landmarks[2 * point1 + 1] - face.landmarks[2 * point2 + 1]);
        return Math.sqrt((x * x) + (y * y));
    }

    static int initTime = 0;
    static double tmpEAR = 0;
    static double EAR = 0;  // EAR值
    double leftEAR, rightEAR;  // 眼睛坐标纵横比
    static double eyesThreshold = 0.75;  // 阈值
    static int eyesClosed = 0;
    static int eyesOpened = 0;
    static long eyesClosedBeginTime;
    public boolean checkEyesClosed() {
        leftEAR = (dis2points(1, 12) + dis2points(34, 3) + dis2points(53, 67)) / (dis2points(94, 59) * 3);
        rightEAR = (dis2points(104, 51) + dis2points(41, 43) + dis2points(85, 47)) / (dis2points(27, 20) * 3);
        Log.e(TAG,  "左眼: " + leftEAR + "右眼: " + rightEAR);

        if (leftEAR / EAR < eyesThreshold && rightEAR / EAR < eyesThreshold) {
            eyesClosed += 1;
            if (eyesClosed == 1) {
                eyesClosedBeginTime = System.currentTimeMillis();
            } else {
                long timeDuring = System.currentTimeMillis() - eyesClosedBeginTime;
                if (timeDuring >= 1000) {  // 持续1秒
                    // 加一层频率判断
                    double eyesClosedFreq = eyesClosed * 1.0 / (eyesClosed + eyesOpened);
                    if (eyesClosedFreq < 0.9) {
                        return false;
                    }
                    eyesClosed = 0;
                    eyesOpened = 0;
                    return true;
                }
            }
        } else {
            // 容错3次
            eyesOpened += 1;
            if (eyesOpened > 3) {
                eyesClosed = 0;
                eyesOpened = 0;
            }
        }
        return false;
    }

    double mouthAR;  // 嘴巴坐标纵横比
    static double yawnThreshold = 0.1;  // 阈值
    static int mouthOpened = 0;
    static int mouthClosed = 0;
    static long yawnBeginTime;
    public boolean checkYawn() {
        mouthAR = (dis2points(40, 63) + dis2points(36, 103) + dis2points(25, 2)) / (dis2points(61, 42) * 3);
        Log.e(TAG,  "嘴巴: " + mouthAR);

        if (mouthAR > yawnThreshold) {
            mouthOpened += 1;
            if (mouthOpened == 1) {
                yawnBeginTime = System.currentTimeMillis();
            } else {
                long timeDuring = System.currentTimeMillis() - yawnBeginTime;
                if (timeDuring >= 3000) {  // 持续3秒
                    // 加一层频率判断
                    double yawnFreq = mouthOpened * 1.0 / (mouthClosed + mouthOpened);
                    if (yawnFreq < 0.9) {
                        return false;
                    }
                    mouthClosed = 0;
                    mouthOpened = 0;
                    return true;
                }
            }
        } else {
            // 容错5次
            mouthClosed += 1;
            if (mouthClosed > 5) {
                mouthClosed = 0;
                mouthOpened = 0;
            }
        }
        return false;
    }
    double mouthToCheekDR;  // 坐标距离比
    static double turnLeftThreshold = 0.45, turnRightThreshold = 3.5;  // 阈值
    static int turnLeft = 0;
    static int turnRight = 0;
    static int turnAhead = 0;
    static long turnBeginTime;
    public boolean checkHeadTurned() {
        mouthToCheekDR = dis2points(36, 66) / dis2points(36, 49);
        Log.e(TAG,  "头: " + mouthToCheekDR);

        if (mouthToCheekDR <= turnLeftThreshold || mouthToCheekDR >= turnRightThreshold) {
            if (mouthToCheekDR <= turnLeftThreshold) {
                turnLeft += 1;
                if (turnRight > 0) {
                    turnRight = 0;
                    return false;
                }
            } else {
                turnRight += 1;
                if (turnLeft > 0) {
                    turnLeft = 0;
                    return false;
                }
            }
            if (turnLeft == 1 || turnRight == 1) {
                turnBeginTime = System.currentTimeMillis();
            } else {
                long timeDuring = System.currentTimeMillis() - turnBeginTime;
                if (timeDuring >= 3000) {  // 持续3秒
                    // 加一层频率判断
                    double turnFreq = (turnLeft + turnRight) * 1.0 / (turnLeft + turnRight + turnAhead);
                    if (turnFreq < 0.9) {
                        return false;
                    }
                    turnLeft = 0;
                    turnRight = 0;
                    turnAhead = 0;
                    return true;
                }
            }
        } else {
            // 容错5次
            turnAhead += 1;
            if (turnAhead > 5) {
                turnLeft = 0;
                turnRight = 0;
                turnAhead = 0;
            }
        }
        return false;
    }

    public Face getTrackingInfo() {
        return face;
    }

}
