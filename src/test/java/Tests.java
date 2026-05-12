import java.io.IOException;
import java.util.Optional;

import com.github.cdefgah.bencoder4j.CircularReferenceException;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.dht.BDecoder;
import com.naelir.dht.Generator;
import com.naelir.dht.GetPeersResponse;

public class Tests {
    public static void main(String[] args) throws IOException, CircularReferenceException {
        new Tests().name();
    }

    public void name() throws IOException, CircularReferenceException {
        String name = new String(
                "64323a6970363a4d4622531ae2313a7264323a696432303a2cf0c440c3d99bc1a52aead72ae48d19e8bab3b7353a6e6f6465733230383a2cf0cb50cdff5b3064b6712ec76ec07a4e5f6b6e0feb752b25c22cf0c81d040074cd05eb8bcd8b33216a8067bb2d7c41e7024af92cf0caa8e76fd6bd67d00fd84fcffb63e89d1b6a2df752cda2ab2cf0c440c3d93c13f738ec00ef2d943cd20342f898352d6b1ecb2cf0c440c3d99bc1a52aead72ae48d95212c2e55c0e3dd541ae12cf0c0f1f1bbe9ebb3a6db3c870c3e99245e0d90bb10bbacde682cf0c440c3d99bc1a52a511ebb497ecc0935de5f9fc343fc1ae22cf0c0fac36b5239c21b1b74305d9caf1869bdef94e3793f4ab7313a70693638383265353a746f6b656e343a3c7b2c85363a76616c7565736c363a2d9a980b574d363a56300b411b42363a56300b413471363a56300b419aa2363a56300b41b1b2363a7deedef9717e363a9a1031220608363a9a1031220953363a9a1031222c02363a9a103122d09b363a9a103122ef05363a9a10696e5b29363a9eadf1083b00363a9eadf108d25b363ac090150b17086565313a74383a3d61db47f3d390d7313a76343a4c540102313a79313a7265");
        byte[] array = Generator.toArray(name);
        Optional<BencodedDictionary> decode = BDecoder.decode(array);
        System.out.println(decode.isEmpty());
        GetPeersResponse axa = new GetPeersResponse(null);
        axa.decode(decode.get());
    }
}
