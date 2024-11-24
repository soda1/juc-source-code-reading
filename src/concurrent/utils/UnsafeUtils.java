package concurrent.utils;

import sun.misc.Unsafe;
import sun.misc.VM;
import sun.reflect.Reflection;

import java.lang.reflect.Field;

/**
 * @author eric
 * @date 11/17/2024
 */
public class UnsafeUtils {
    public static Unsafe getUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            // Field unsafeField = Unsafe.class.getDeclaredFields()[0]; //也可以这样，作用相同
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            return unsafe;
        } catch (Exception e) {

        }
        return null;
    }
}
