package com.naeir.bt;

import java.util.Iterator;

import com.github.cdefgah.bencoder4j.model.BencodedByteSequence;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.github.cdefgah.bencoder4j.model.BencodedObject;

public class Convert {
    public static String to(BencodedDictionary d) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        Iterator<BencodedByteSequence> keysIterator = d.getKeysIterator();
        while (keysIterator.hasNext()) {
            BencodedByteSequence key = keysIterator.next();
            BencodedObject value = d.get(key);
            if (value instanceof BencodedByteSequence bbs) {
                sb.append("([").append(key.toUTF8String()).append("]:[").append(bbs.toUTF8String()).append("])\n");
            } else {
                sb.append("([").append(key.toUTF8String()).append("]:[").append(value.toString()).append("])\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
