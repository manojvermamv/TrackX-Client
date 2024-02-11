package android.os;

public interface IPowerManager extends IInterface {

    void reboot(boolean confirm, String reason, boolean wait);

    abstract class Stub extends Binder implements IPowerManager {
        public static IPowerManager asInterface(IBinder binder) {
            throw new RuntimeException("STUB");
        }
    }
}