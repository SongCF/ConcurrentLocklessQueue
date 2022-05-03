package org.songcf.barrier;

import org.songcf.valotile.AbstractVolatileLong;
import org.songcf.valotile.VolatileLongValueWithoutPadding;

public class CursorFactory {

    static String cursorValClass = VolatileLongValueWithoutPadding.class.getName();

    public static Cursor newCursor() {
        try {
            AbstractVolatileLong val = (AbstractVolatileLong) Class.forName(cursorValClass).newInstance();
            return new Cursor(val, Cursor.CURSOR_INITIAL_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setCursorClass(String cursorValClass) {
        if (cursorValClass == null || cursorValClass.isEmpty()) {
            return;
        }
        //校验能否加载类
        CursorFactory.cursorValClass = cursorValClass;
        newCursor();
    }
}