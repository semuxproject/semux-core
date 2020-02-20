/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeTest {

    private static final Logger logger = LoggerFactory.getLogger(NativeTest.class);

    private static final byte[] MESSAGE = Bytes.of("test");
    private static final byte[] H256 = Hex
            .decode("928b20366943e2afd11ebc0eae2e53a93bf177a4fcf35bcc64d503704e65e202");
    private static final byte[] H160 = Hex
            .decode("86e8402b7615f07a2acb2ef1f4a54d323bbede77");
    private static final byte[] PUBLIC_KEY = Hex
            .decode("04dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] PRIVATE_KEY = Hex
            .decode("a73994d748b936ce63ae254508d909381fdbec3bbc11f4401630414f21fecb1704dca6ea3de4a96e952ea4ce178ba5330e7d1a25507c9195455a50aeb93220bf");
    private static final byte[] SIGNATURE = Hex
            .decode("cc52ae5b72af210073756f801cf8ffa36cefe96c5010b2cf25d04dfc5b0495e4ee3e14774c4607a4475f2b449a3181c9bd2c6aed46ed283debfebe19589f550e");

    @BeforeClass
    public static void setup() {
        assertTrue(Native.isEnabled());
        Native.disable();
    }

    @AfterClass
    public static void teardown() {
        Native.enable();
    }

    @Test(expected = CryptoException.class)
    public void testH256Null() {
        Native.h256(null);
    }

    @Test
    public void testH256() {
        assertArrayEquals(H256, Native.h256(MESSAGE));
    }

    @Test(expected = CryptoException.class)
    public void testH160Null() {
        Native.h160(null);
    }

    @Test
    public void testH160() {
        assertArrayEquals(H160, Native.h160(MESSAGE));
    }

    @Test(expected = CryptoException.class)
    public void testSignNull() {
        Native.sign(null, null);
    }

    @Test
    public void testSign() {
        assertArrayEquals(SIGNATURE, Native.sign(MESSAGE, PRIVATE_KEY));
    }

    @Test
    public void testVerifyNull() {
        assertFalse(Native.verify(null, null, null));
    }

    @Test
    public void testVerify() {
        assertTrue(Native.verify(MESSAGE, SIGNATURE, PUBLIC_KEY));
    }

    @Test
    public void testVerifyBatch_small() {
        // votes of block #1
        byte[][] pks = Stream.of(
                "0xe9b59e9ac73113a365a20deaab444bdd916fba5a7400be97d0b1a16563cd2f09",
                "0x85c87ffdfc48b5c072ca5a9aa0ad618432161b37759a3d7aa7ff18ac18d9b51e",
                "0x9ce0e4c12239a067b20e9ba7cad636b043a547ce7cc0088a123c0ab089662766",
                "0x99dd11bce44b02023767cd8250a430880544a6305a06bccf53acf6ba58be13ac")
                .map(Hex::decode0x).toArray(byte[][]::new);
        byte[][] msgs = IntStream.range(0, pks.length).mapToObj(i -> Hex.decode0x(
                "0x010100000000000000c8000000002032da907fbec0da4c6c50c5b22690ebd81f0b494f6a88a0fd95626c0705391aef"))
                .toArray(byte[][]::new);
        byte[][] sigs = Stream.of(
                "0x2d91d1384e76186668ee54e243b8ebc95a658116c4ec1b62ecad475b3632cb052b9111ac8cbff6a2ae4660e7d9c691bd44e131cbb6de52bd7529593228d6ed09",
                "0xe74de0cb8b45b8a584c1fa195217e0cab3615421f26680cbb502dbd02067aa3f4c017e1d2f5b2c81bea109492307084026d13910a953adae5f8dd9f938f5d00c",
                "0xc06cd6d0bd33b978d89d2237088bf08b7e27db088d674f7d1f5bccf148032747a2150be78e3794dae6ed8465b403cd2a92ed6a1a8a84803cc57afe76feb9c402",
                "0x3209ad948e51a21c9a02b64d47c734278c574192eb70923bbb15551b1b780d95fb897db3a3e1e2a13240385d6aa5f1a934305752755dd3be33b394e062efcc00")
                .map(Hex::decode0x).toArray(byte[][]::new);
        assertTrue(Native.verifyBatch(msgs, sigs, pks));
    }

    /**
     * Using Native#verifyBatch should be ~40% faster than Native#verify in this
     * case
     */
    @Test
    public void testVerifyBatch_large() {
        // votes of block #90000
        byte[][] pks = Stream
                .of("0xbc592668cb5fd4e3d7d629f7876b5ce3a140cdc2194dd717b6a911e98e4bb01d",
                        "0xe9b59e9ac73113a365a20deaab444bdd916fba5a7400be97d0b1a16563cd2f09",
                        "0x3062ec08c932354bcc9f39f31100e657f98067ec0a00ff00cc703f2cfc011a60",
                        "0xbfed99ae1cb501f3ea1b892e52938ebd8492befa7fd90a669d91fbac60ce104a",
                        "0xf81af3334de1adbc28b7877a93cb3c757e07c694cc86abf0a89995a711baa0d1",
                        "0xbc64b0864a8d31e606a05ea028fdfab1a51baf2a882f1e975cdddcd089b96231",
                        "0xd29203e10b0e44627ccea1211d237d2af063bcfa1dd339e0c26dac34abbf4a84",
                        "0xaad9acb3ce010facec09d920faa4203a1f438a11f03c6d8adada1d5941af50ed",
                        "0x75ff5e56b613ca400e2b4fb24420105eceb14ba8844d9f27f68a09a3fa0300fa",
                        "0xd7b29b7d7de295a96361d76fda2179f954ed82250633ff29b8d5e49967f9c9f6",
                        "0x6f874815fd4e35c2e803ec1f04fb21721e3b475998fbbc98a5106e349721c519",
                        "0x441abe52dfdb8c39c7a6e07eed06b779705948c3c982ca3982470e7e753e2b29",
                        "0x9370ecea84ff948482bcd41b85df60ba72dc062a7abc615b697ef2b75ee1a1d6",
                        "0xca14f6e417e5fc19cac27763be76e9df0c49e34f2aa216340d6941520d40013b",
                        "0x3bb5951ab53e0326e203ec747a95c6a518d3f7e6dbb6cfadad0a63304bc7ada8",
                        "0x6819a8444df5496b67b98db1bbb80e5e30458524cf8bdec125e1581c367c20a9",
                        "0x9579abc46c96a1fb7a0bda1452804c43f4da4d4e0844142d19befd227bf90d5b",
                        "0x0295602952125befd286575c2abc10fd6c8b32d8d656de46114d516cbef1a684",
                        "0x662de9678d3dc5875e80b279b44492873283ce79c9e77b755b0a1215e86cbd16",
                        "0xd76768fbddaf04e09fb31137424d0f43639086ac542fdd5843857d00ecec05ba",
                        "0xe88e93aa69b2d7dcf881d44702c15c6e2e2006194e801a19784969511111e05b",
                        "0x24373b474d590dec7d70be71cea3dc56069f25024abfe3a02cde28a7f9e008a8",
                        "0x49e0f8ecc6a8c4c8c7b24a0f074fae420611aca2b6082760b3b86d8b3b514dd4",
                        "0xe06c3889ce098acdac37b35f899997a21360647141c837e4c091afcea16833f3",
                        "0x53174d23de0ec66d7ae3fd57dc6d3fd0f8289fac14f769b3a9268ac1fe1b3f0e",
                        "0x122682fdba22d9594d103abae7fdffe929cdfeb9ab85ee0b93d6c6574977911b",
                        "0xbc59f72a0859a3ff02f38fe27bdebe8e58e0d0bcfe21a74759a8e9fbb7c5f250",
                        "0x9a2302b4fcc530652e3f12b90c82c40442d0f47c9ff1b41e8cb084357b771517",
                        "0x4d32a519c3ebc05f1a89fa876d1a66cfff38ad3246b0a493e973094a3ecad268",
                        "0x1d75044a65d8366a9ba3de6833f64219a4285f4a07b16d6574b57c7d5126a732",
                        "0x6f7c67410b0e9bfa337fcb1a89a7d04855e25a2f6eb77cba42096bbbd64e128e",
                        "0x80c0b33eec55a95d9bca896ff84f953e4720a8d6ec2e5ec3e4137a408e37219b",
                        "0x29015533190ff959785f90d8143929dab044517da22cd0213f758b6c805e0432",
                        "0x4df64002029e7878847ac9fdf0488f69b49f1046e03e72911e7308f5b3136832",
                        "0x99dd11bce44b02023767cd8250a430880544a6305a06bccf53acf6ba58be13ac",
                        "0x9278f568e8053eecdb6a4b775943787a264c6e0e076d3eeafd7ce33a416f1550",
                        "0xfe721162b1e9f9830b9c7a3d734582aaff3884e1bdeb906b7f3c3ff30303f5ec",
                        "0x3a6a74ef2fcd4286f94b7723cd8298ea2f04dbdef7c9458faedc9e35ba67ac5b",
                        "0xd5b5f39bfe00c48a2f186537ebf3a1ad380cea4bb8585a92cf882f5d4533d4a0",
                        "0x2b4bf0074736535e023705f1fc7d2d3e9c1792a5dd6c40bb279e146d7b98659c",
                        "0x2bebfc341485926e406a6594024d92e2ed2316ac82eb332a0641d5235a925c4a",
                        "0x4d789ac7d1c2efdb6749ad20b16786fb14761e7486391dbf69363c308e047b43",
                        "0x0ba09dd49e6e95100847370a2a08c8c45844751eecdd9e9df3a51a6597b37ffe",
                        "0x1a1e849c5b5ec60d9c7a34dcc98cb72a1904bf2599f64d6d129867c5b7c99569",
                        "0x147d0d534a340ff51abd213b2eb6f5d53a87f2be949ad3a7466467af37efebfd",
                        "0x307ccde72bb0f246241f1c28a21f8e698a5509909131128435b1e42619e651bc",
                        "0x8915b2c44a98a82d1e7ec020573bf91f15673dc0e661fe30f8dbe4a3a49b2ca4",
                        "0xb2d8b1852ea44502134547c9c33da4b88f6021922bd0dbddc8134ab175dc529c",
                        "0x72a7b1ca4b57458d789b5eee5617cbb4e9b2f2a9df8f465845a5b86e81277beb",
                        "0x5fc3db5f2d0d563eebe5180b003489f413aac5caf8763bb69bde0fe66f3d0fde",
                        "0xffbac0b93a11e47fea21b3888fe51518d25ccb9b38ba5d90b3d7236b2add0065",
                        "0x165e618096a10eb09dec06e83f1217a799c8c95d0074f3f2890200e91ed22ebb",
                        "0x71e7a8885b978296f7b05362c12a10e571ba17c958f35f2869c7156ab97b0ad0",
                        "0x17d117b84a2ae2269f7795b2049ae0960d60d6840a49850a7391df32ca06bd89",
                        "0xe4341077892314aeeba41336bded27d870b66a78d9fef1061e9402437f743065",
                        "0xbc2c9e218153030c74ceda69d8e1623a60dd0dac7826e45095ed124c9f9eadec",
                        "0x85c87ffdfc48b5c072ca5a9aa0ad618432161b37759a3d7aa7ff18ac18d9b51e",
                        "0x3cb958e61211453e4e11c247ef396b98d59834fa74588860cbba8b4b2abee286",
                        "0x713fd61cfc7c74a6b969f34fa9ed7e8a247717436612336473fb2a8ad406609f",
                        "0x0213310c980ed72d13c4fcda57055058d930d3e415711e622e0230056f725168",
                        "0x3deb4c12a2ee12d6749e8be62b0b480ee54c6b86c5e4af98ef439c858213d53d",
                        "0x1235174e0d11201db8c52a91464336e358cf87e7247783854135cea9ffbe8612",
                        "0x582604b2afa1e8fffee3499916d0ec779e4edcf9a0315d1837283e31f831ab47",
                        "0x76692e12acaf8343523cb792eb5fd0590279bc35a69fa5c4eefb31d6b3afe037",
                        "0x84de85388fcc77be4aabca4c479d8e50526ffb8ba727fc160fab54f8febc63d9",
                        "0x22a1ca072be7e4eae3f1a2d0fb0d8d7e5d41ba87cabefc5cc0fab87c1024dd3c",
                        "0x812624f2dbb1dc86331d08f11e03136f749bc62cc265beef1af52d3354021e4f",
                        "0xd4deacf93cb16ce6ffdeed65568cbe6642f3df93ab85e93c05258918dbf3ac8f",
                        "0xb2af4f9aa493e4325edf5521ca792667c23e6a6bdccdac4034095c5afa5898ac",
                        "0x5c1c10d88c083c21fe82268a8545b8af4f6e13cc2f6acc0e8752aae3075a066b",
                        "0x6d2507e14f531e9011eda3d6b6425a19b0d3dcb830bbd5e2b2975fa2f2fba41c",
                        "0xd2e2bc895bc8d2a84406615e6adbef96bb0c553182a3dc3c89439564c1765109",
                        "0xa3a0e8027042043c76e84e47662eea0e868e648a1092be986c4664fc95ccd74f",
                        "0xa519e5efef8114a343476a87a332dc51585cbd0793b4c567792a013735c4070a",
                        "0x9ce0e4c12239a067b20e9ba7cad636b043a547ce7cc0088a123c0ab089662766",
                        "0xc031ab8e8c32e01386d476592b769be19671884ca17bf270aea1c99416982893",
                        "0x63ea87b850369bb8926a84018547821d1442c9d0301fcde5b24d94179460eb99",
                        "0x3417e9fb14d8bd73a77e2883dd5320de0cc04028b1bd5c368340396d7e9d4954",
                        "0x1aea13c8cab46fc3bb05f57ccc0ba15c61fb3478c87665df8c414b2ba04b9250",
                        "0xa3fa0db24912871e0f9d7197abda882ff239b7d4343cedf410005a3af835dfae",
                        "0xe8bb463276384eda95f948be1098a2ae7f92ece0f238a0d3a3ef7218880f9cf4",
                        "0xef148d50ea0dee5bd48dbc9b37022e2e356a3e7fdb89e2ee2e6f87ca6d73bbd7",
                        "0x3e1e9fbda0bf293a76336941c1393c1fe7012fec391603227cf871f704ee702b",
                        "0x59aee04c3ebe903a659034cb5afaa680c886f5eb67cc8edc8dec23c0a3dc86e9",
                        "0x7785041885b43d803a19eaf468729d1d68afabc9989b51faf0f917340961b4e1",
                        "0xf5a85850ae17e8d9de1f7c9d9fe230a74a5650ee92db6b7ab68b63b853055620",
                        "0x0f099bb5857f72632b06deecf2cb8a971a6dd7558d1ecf99ad7c595d424de63b",
                        "0x38a6f2c50ab111553e930d046aae8b0ccc1e06b83a4fbf61b472b25319d09cc8",
                        "0xf968bb2096f6d5f247ecb72eacb1884b882c1ec667bdff06617f715c31c8d80e",
                        "0xdfa09b28acfa4458c33cac13b9c34ad8286fa652b72df24dedc7d33eb04f37d7",
                        "0xd0d8f30b976d78682f921e133c7f7024021cc3a57b82b32530c6ff151ab5bc6f",
                        "0x66a7bd8642cf476db48c74b67f9cdb58a774c76cd0698c65c97a8f2f796204e4",
                        "0xf59ae278164b844ed2240716c42210c72d95bfbf7bed350407c9fd656ce9e88d",
                        "0xc59f8e0190fa1f1c6511e73868b88d2b99bfc3957a3198cf77da354c93ae4736")
                .map(Hex::decode0x).toArray(byte[][]::new);
        byte[][] msgs = Collections.nCopies(pks.length, Hex.decode0x(
                "0x01010000000000015f90000000002078af7ef656065e12d5c48487d0b8459798615ebe8891a71b0727710ef1bef6f3"))
                .toArray(new byte[pks.length][]);
        byte[][] sigs = Stream.of(
                "0xb594bc56d7ab769f7154b8eaab8d482d0e2fdaac06fd6d5ffada9faa39e40ceb40b720f50152b39cc685618ae16eda35c93e1979995e091640ade35f4731780f",
                "0xe97b0b8feaa071c32e69ca4cb02b7cddae14722d3b5c29f9d395eee1009d3a24215aa8573ae12391fcd403d3411b98c48c48a43978533ec691cfe88355c7000e",
                "0x0412dd1733e369335b1a2e284da1cd16c0b18213a3c0c1eaef92d20ca117e344df9790619d2be942144084020962d7686b4f2bf320a2521ba92d1010f22f530b",
                "0x2558e2d023d938e6cf0bd76e1eed79d29270c378d731bc8f9d51d4f2cf06fa65151cc01d09298ab793aed86e0c2b83836c710422700340e3720b356353e2a909",
                "0x81efbdc3549bec852f0d2b9426eb69a7bdc70fd033d7892339271c5a486006cbf03fe7347a804991c831668ecacb6758bb9bd4b034ce81d21cf420f8cf6ff201",
                "0x4901b0a226ed11e6b9b012016b945436caa5fbe658437332f9613a13d3f3997da2bd093c0047f1ca3406a739cd1a0e048a3f7d3a2fe36f251da6550a38718807",
                "0x4b8b573d1c950e3b9a1ce8537968d1b931b6f09f98b9c53071d1d290832701892eb9e69d380e0d70c2e4f002ded71fd81f531fe083357745f5f6db31bd4d0b05",
                "0x6521f69bd8261bfa60e0f66df53d885821f4ee8a8b99287ae232cc115451fe0d032bcb4ef29979aaaf4af9e59ddaa2d6cdd1c1569b10942a3ecb58ada83f090d",
                "0x41a77c64ee4b9a82e8bb98a761c5d0497e4b86212c0cc374abddebd89fb821af480a8446458270deabca4b694751a56f96dff14af5b01c7d0228b04cd854b701",
                "0x9fc5ae565cbbd576b82ce7adf3f046f459bd54afe5c2df95f72d6509f1865d23086792dfae339a008824173676e4af9f4d608fb4a99f2f19d5b4f61c61fc6e04",
                "0xc4dc1031e72e55d340e7e48653b4a36c74a8d56225410895173a0f094ed0c750161e386c6003fa3df6e6a325346b3d552f40818c806b928d499390558576fd03",
                "0xe99910f345e5887502df350667d9543b642387535c0fe3fd78faebfd0ad84122129be43172b4b99d166c39ca1cca91efee4247b3d35a55c7e6f49f841be65809",
                "0x941efacb2b20daeaeadf32e963743cc8f139a90a0aaec0c625e8c0c1ba3f7c2f9b887d466a54f23d9d074380e5fb230ccfd1b94644579ea27acb5655744df403",
                "0x2b58c5a24b7ecdf82d9d19923d51eab69b937e9dbc8e33430c7f85140ef43819739480c3ec553ee1961c3f53795a71d7f0c6dbd20e9deb6f9a56ca9b6c7bd301",
                "0x127f18f305f479af1983ba2529a975f58dae51ba3a063f27865bb6347005a9ac95ae64143fe933c834e7687311bfb44345d96295e561c94851f4f955762c1708",
                "0x3b64b715c5423cace4fe4b0284890c86b4ba5847f6c30f022a0b209d04737d44c881db315f73114a996be7cd5c22b05bfc8efc7264ce3bfdca33102df501a306",
                "0x55d355aefcbf48415bb494f44e225775cbf4b94b20d0e694d10a62c814e24ab63e9eedf01c4c48c63e2a6171082f8c517f37bf9ed4574589d34949ac4c658107",
                "0xbbd9d36e56888e2252262f1dd2e1ff93bf0026d495cc95d71d613e9e7267e368bac025d26705badd2143681912012c05e035afcad83c5370369576dcb061bb01",
                "0x48cac6c6f9c076939381d52b4a07b746e3da26ab15f276b6c5d2aa3f75c8c25d2e6dad00bba7bf80394659c130b43dee2f53e4d951e218f664fcedd04d537308",
                "0xc61ef6a858d20e05c2699cb0022408ad4ae0c1f55245d1bfca17f2a6ebefb4d74e1c768e7b35f23457f09c89dd2bf2dfa98cbc31ce42d2b96c55fecdad797708",
                "0x8343c9ebc2d281935be5673f3f416ffd4bd2b2422af2d884ab510578396b8cd7ca9e4444199a23e51ddaf0a6bb484dd5f4f5dbaa7198179137914774b149b70c",
                "0x420160df117b0f6fc5c7fa36a04134138029721fb869895f8a6d0d19162528210109101430a1df94306c8acf8f229be4da1413bf19b01e5e69792e906649c308",
                "0x3d338061ff1ec94d9186158b7dbc7120d061c6ad1e787871f38b392819f0c6303b66480daa2f9e629f59dee644f9958bdf803a7ecaf9256befd5521e8e631b0a",
                "0xe8519bc3dbf134df36ada341e803ffb404368ad693591711eb8785fd73265a8658886d2ece824ca3318f977cf87cdff38925b6e75ed750cee716d2fd5b275205",
                "0x28c7dd085c71e5c83eb7e2db734eff294979a527722cef438133deed608d0df6f6cfff23b9d3815e1d17d6416390fafcbf37ac9d55595aa7e7194798429ea500",
                "0xf5e1fec868f5082218cdc48f438c1f7049cc1bec87f363aeb3042ac7fade7f67c3cc8b1ac6d587e1a99f46f1f6f7eec442e08c122bdc130bb204acf5c316bb01",
                "0x5efe1ca1116cf980c0d7725ab0100dd66bc2c39afb266f601c9524ba1610e681980cd5b27669c3b1f2b40db89462bb9d1a8c6b9a10a5ad7298cb9738b36e730d",
                "0xa2ad06a6ba320b08ad82d155b5f2541bd506bcba99229f429023f40a93bc4f05e4f7083836d0595bd6875931dc34f72382ce44ade0b7ffe550ddcce00b861b06",
                "0xb2043ce84bed0e51db63d6d2ae6c571deb0689979f61a360258bfc63e2c37ce60705246a778d55923e598dcf141cdcd1a4613b16434b9ed5a2900d5049712604",
                "0xfbea68eb00d120231057a654a688b791ba08629c7ea85cb54815b235c0c44ad9ea2abbcd50cb5c1bc3035f8ba3cf92c379b38fbca317debe9a0b39700be8b40a",
                "0x8c9ddc53864a927872ed03c03bf3aafaed6ee411d38661c22119c7d1d7e8fb64a761cc48bf5fcd9da799e6b4d3703cc8ea7736f6b9692d2ba2402cde4a370707",
                "0xda1dafb6ca872a56ddf1250f5c71c3be63bd057582edf9186eaab3ee0f7d372284be1bbd9b8672404a33effc020731e902965f60454922ef27df6092c7f4c205",
                "0xc045bf45b6936d3089bbe13043821c7ebc70056292db1daa9cbbda8b56c8573430683dd226bc95b063035f0055323ae81f8ba98532365b0299860a9a1fb8fd02",
                "0x77f32cbd189811238a5c1dcbc7d81f37dcd1860b1fb139301a669eea3bf086acd8a761722593d225c1e0a5f3feb424af6a38d039d3e8840d9de1ee76e65c4e02",
                "0x899e600a3879c1abd8ae2db8add56e61e8d45db7766a29ad49686f765ef1f7282c89416bee6649ec301ac902291f354b9982b27da7295b161d6a4f7cf4740509",
                "0x8f7e41d6ad1bfc82286b5a72a9c05e37d7fb1f0fdc7795a1dd065700da1fafdf782fa49cac7d1f6d9620033cfb68791e24a83aa9e3a439a1c9ec02a01801c601",
                "0xeac8a6c25e6d96c13ef3aeb3966542a8dd5dd05ea912f3462eadffc19c09f7cb3a2aea04e9b5534fbad081249de74e8e03c29aa2510c44cf1affc7c1d68d2f0c",
                "0x22384d9a32a85f6cec901d4d5577434075c5367368c28bacc0d25ede800aaf3f213ee9a31e3c9cc978bb0293b38f0360241912466d066b57e1d6019a71f03600",
                "0xc38b15f648147dfdf9d95fcde115860ead11e6bf611b1fce4d9aa2e9530b797539fb93198b23f61674e0ac36c5223b09c369b14ae1b1056ca633dd801063380a",
                "0x76c03d3582795578e98b0b62a70611fb061eb68f7d610670e60ed586a2414c0dad35b716a2a8432be13b2ec7086b84272e2a579998b212b99a495ebeb64af706",
                "0x6b65a45e3d2a8d472362317f93587342ce82ec2c731d6c43cebe7015c674a5948eb90103cf6642883ae78b2f9d67b6de8234d8c80233cd6c3ddbcf5dfea3cf0b",
                "0x5a6dfc81b27d317b2563ef0aca95a7b50aa0ea2203d28ef2058220db04becb3b0faf3de2247b75d8ca59b9d51dd933e3aaaf85709dab35bd1c65451917dd2e0e",
                "0x1f67169a1bbfc26c1df1f4ca91e740733f9aebb8157dcf928fa8ad09da2177c972af2ca5b1d06aa2f6711d0eb0929bfbdc8d15bd2a4ef38ce4d8ce6815c04c0c",
                "0x70397a15a28c79ef2815f16f886e4d0cae1ce5b417ddac174ccc2ff6271f4c2c8fbdd61f34f7a38115f611d3ae3f2023e6df011f26aa0795eb095ef6bff0a202",
                "0x03f85bb4d8931ac031bd9bf133a57a7ddad034adfcd20f370a2865a715f6978a5c7986d515e3cff56eb48a12e2ed99952b55e2c98367a4b48fd619b58fe31b00",
                "0xb67e3f6d8eb7d63cd670bf95ff141ef2f813600a2a3a6def485a2ddd435aaf957e69bc1d0650854e92603a5da5f756326daaac25984b096cf5991d3d92ae4a0d",
                "0x718ae8b690f723cf3a1d53a040e35b94a38bc7157e22662986a6fee4764634ea906797bcbd687e502f90dc6380bd02063e446ea7b7622c947c6cfa20755f650e",
                "0x6d02566bf468913e78dd4928e7441dff0821dc805e4a46e5d5d64dd3304801918b05727198883de1c6f62d52dac35a83253ec05ab7fd75a74ec971fd6040c30c",
                "0x21b407b053258d5efe79482aeb8816d7fade9466e8b8e63d7e6263d727b20e12f4830e4248b6037c725c584e12e44c06fe26bb40a93e51dd2e2fbd2b5567c806",
                "0xadf2a38088e15e6b13a5f51e96939347dc8dd5fbbc1b9d1e0f4ea511ea25f22c563feec779119f44d04cf44867f8003eedeba5e705f59f0723a29dc4ad805e04",
                "0xea0597a6b449c001d427c92490008240d1f6e9e8fadb7e4cefcb75b61bdf3b717cc2b5f2c08e82e5a62095e0b48cb8324ac9679e2d1b2a4618f85e3f6ab2e10b",
                "0x1730b75a93191b06c5319fe0dbdff9a2844764024835b879a193f13564b587d7d33354fa3e4b1c40cf4d0b37eafed62116847359b9a10e7819171fee326a1f0b",
                "0x2b3aa3daff5894edcbf58407169ebef4ec9356e6b38a5a03e0370dc33ed623f9cd6c9b46670b16a4afc8bcc9565f2a57a410ccb5a63737873d347bf09223390e",
                "0xab717eef97e9fb67b97c964710d8aa390e42ac1759850127798725fe43bd6a442ba42b8ad13eaf8948027d50947e37e6e68bd5d1bedcf77afa108ee194eb1805",
                "0x6d2e9cc0e4103edac8f41689f691ccee41926f3df0eab70c6bafd6ed81d1ca3fa644781b745578bfe7017009b12c1820695ef83f3d43d89db51d01f9503ee109",
                "0xc1b74d00d672c4831c5e568981c967366150cf6ea8145441f302ce680399959edf469566efbcfef6643b8ba96bd6e3c0ad696e6cef7c6f98a7d4991e857b8c0e",
                "0x3a75330cafe9b58674e58241e769a71c056a8d6e2e542a9855d84f82555bf66fd83466154815bb21c6c6c6aabf2e8053210720780eb30a69bac6d3816b67cd09",
                "0x52d414984845cc932e869f8f50860ee90e33b2ec37f5a3848b49f7dc8b12e6184b64fa26d9f082a239b56e558e7c9c0b47af5192ec66b32eab21f00840cd820b",
                "0x01ff62eec8ee8c2da60bc16c051662014725340ec50465d0eba97c0545f394951586a454b0dc5dc4ec0c30bf1d5612e4503602968321f11267803e41e77d1804",
                "0x37bf57b66a7ae98ebd0b7b3b8d745fc437e202f3e3c2ef0942bfc86fd379dd3c235565fc546a4931532290a16c8ef7131ea13b77580015dd318533be36e3040d",
                "0x3c720327ab1da7d025ebb612998bcbea8b19d129160ae31c124dc9a0cae594c580f22d4e267a0846e176050d8b135e19bfc61c75579523e095d4f96e88a45208",
                "0x883e91131b0b6c44706da02711ef65950c1a76f14d4c62d44f3b419c793b6471384f0ede877f05897c9d949eca657dc886d020b4754e6a9a17150182dfdb7400",
                "0x6db32a7e8f64553a1cc31fb6d2555b10395983cf0491b5b33ccc0816a1fe875d1231cc2090a939ada932c9940a47e578884b1bff3e68cada9d1cdd615c8ba004",
                "0xbbc6a4135685a3c5c8f182fce798f8aa98b5aa5ae25d299181b60ea286fff494620ec8daa18ed57bddeffb8de5caa9fbba5a631f1aa3b5e56e94b8bd702f0008",
                "0xaf6aa20a33472c08ab28b81748aa6c8b103032dd488029ea906a97dd5f8bead76fa9e9af08eb0ffdcbf585cf5d0a5f3214837a3fdfaa2ecf89eec26d5e567c0f",
                "0xfd5f68117a543f6e233d6df6860297a49267438d6c407582acd8c3949397b0b66a9e969b1207479d545d5bb754bea989687e739610af1aca45ee801b3bc4cb07",
                "0x90c08262a095f5503d0462d4a8b397ef00fc6c5a245f394ccbb0f6b52fb99c5dffaf6ff36b076bd867c7c42b4d52c9a9717b0809dbed07f03db16948c44a6004",
                "0x7d3ff381e6e77a59aaf1938a978ec2e71186745635a41fa4cc740800264e4d2cb88ca7a3ca7b7e4a5e66fbc86264d32fd595f9306263563dc1ebe163e477ef0f",
                "0xbadc7400b1f95aacb2902480150a129463398aed4a2f4f88929dd29a68abe08f6fac9991e5afc27df89666abebb127133f6a45f0aacbc32e51620fe27c89d40b",
                "0xdf9354c478b74e5dfdc3ee572d16d419bfafcc7ce6ef6c9eb67832f94d43e57df795e881729e92c62dee03960e4617c527be1364879f9274eceb5dbc732ea506",
                "0xc7177e73d3b0bf68ab66e6fda466e6ee14a99b726c07cf88351837050ad9c9071d339d5e02b3814d47b6fd43aab69173dc355534da104859d3625f5b71797403",
                "0x8cf4744b60a6f7bbfaec2fb4e936e3e373df9a5a37ec92657b972aef01c7576fe3cdf24391d4586db2590ece29b56b737a076d1184e3208d5864912fd048d70a",
                "0x2d9ebe3ce92b34b37edb4e0e265a0c4dd084c526e131b3822a7ba1dbb42004ccd403c09dfdc2816ac972e50b3d6c91d15ae547b32e3d062de065a3d9280b6107",
                "0xd4741f630dec3a1835fee1bc7d4b38ede6008ccc77e864447de6b745b33075a2749418ff407a1e4f330796adc6ad40d9f13b41ffb301f739934e03b704c74d0c",
                "0x364cb8d31a9ac8354464466c29d45996a65c6fb097021c2f2e62ca9acb7de0ac7157485dbd99c4fd683974fa2e7f45b5f2784b4868418335e0e4d6d353d2830e",
                "0x3556ee0fa390ae4592789d4cf5f10b637418c42e8986f6d610fa6ff52ecff4544045694f74d4b0994d5240a530d0a6165ab981168cff1e8071aebc7481582408",
                "0xe7c54e0adf846588d9d9481458fe465e9b22c00f58ea09105184044ebd34948439024d2c53aee24c9d79b8262ba8051ca7e65dafcab6ab84b8422b23f02fb708",
                "0x54b0c8e4bf40c3b73ce3e4b79bc029efb2422dadf71fd94ea64dcbfd0c641b287f4a9777f5a15e99ca8f025c73f0b7edb2e3c206a712387b049b924f78cfe10e",
                "0x2be408c4cb9d5cae1fdd4d5756cf3d06061f94da8afea4ba2943b78407a100b9a20f403212e713cd06672af5e97e948e4c179305865479b0ec5fcb77c657f30c",
                "0x01780bf5a4fb9d9fab37f08dacdfcddc1d5bc7cfa018c6cc8f216fb362e3d82e08e3f8c9eac10be5742467606bcdaab7332926110ad233bd460f1e90e51ade0e",
                "0x4196cb94e8b3c570bf2fbdbc346fb2d98d35cd943a5481fc05cc69745819c14a1c5079a8cfd3fe6122f8889d2736cc8b4564c73bae996c1b4e6320955d6da802",
                "0x99018a081b684e6c1aa2bdc2fe8e7fcac638f62dea7e99ab0a3d68cbd67f22db75b798ad984cf55a84ce9e8c4d6ea1b6d041699b604d29be6c34c234424e570a",
                "0x6b6e9ef87bec6f9cbd32843d3bce9e519957db5d5480d41928d4931f4ece3fbf08061481e0819ce2a1e7048faa2de5a7cb209148abb236719932790311ebea05",
                "0x24c22a9381c87e69caaf86908bea491bf8d7e93a8a26b606022ca4da2c61e96766306bf8679054035ab717e24e6a48259fc96bf7709719f3f0380373c828e002",
                "0x0caedaf2a3b90fd26a1e6d88b86b6075773cad6ff4236bc34555a2cd4ea15bfa8f871657350937b12ef9a344bbba2e79a6d0c877258b2baf2cb1b53ced09460a",
                "0xee1e40cd911bc42d9c9e4bfbbd8af0dd99f922e4b4e1580ad53083d987d238c4afcdc20dbe3cbc2b3c894d0114bb8f4582545c1408b8690afcc25e396b5bee0f",
                "0x158df5e63ea253e16fcf8881a4d5035cfa2ab5810c9c9ab7f02d42e304e0c9f126d7ab6a313c6dc9192b0e2c81ad701b7a0f79c062ea5489508adc5820610e00",
                "0xb74ff95ae9637ab12eb456bafb26c913adb5ee9f31ae3c7a97623e36f257134f94fdc2dcec60ba917bf29e6383d10f2dacc3c58c579afd8fc73c330cbb979e09",
                "0xb39d2f58fb68d5aff5659857839e932d171c579e8fb7307e828244e6031418d4456d54a4490a538051d0fc7fed448454252f54c1afcdf3cc3db232b3bd86cc04",
                "0x1b2781562223e30687bb909469c798fd008ac0a412c6abb00575299de0e30c6cda935505eee2c645dce3f3c22c15dacf5028915ef14cb0f186e8f7508a874106",
                "0xb877bedb76992a3dbb285fa70ff1d4635b3a2d010e4e3e87c92faa482fab6618739e835ff1d3b0a93caa8fa52141a910c7e5b2e95be14e56b539bdd31e8aa706",
                "0x966de3097191c355f92100c1ad6ab57288a9dcc5607c392f0d5651e792a17bf7ed81eeb84167c5da6d025af94cb3f4b5c57481d256d0d33b3d9e81877e05da0c",
                "0x0a50f1a5889a9cf5f3b24f2c0123a12c592f44ec7640165239ccbaae3b892a6eff504dc00b48bf665f116040371ac51b6360fae51dd75613776a148edff75407",
                "0x174a0bb25f69c0d4ae0f7a4bffcda43832738557608b6e52e88753e456e591b19b5873f688b2f1d1bfa599322b0976ed62c2adfd611860bd611df2c7ff0f7f0f")
                .map(Hex::decode0x).toArray(byte[][]::new);
        assertTrue(Native.verifyBatch(msgs, sigs, pks));

        // false cases
        byte[][] invalidSigs = Arrays.copyOf(sigs, sigs.length);
        invalidSigs[RandomUtils.nextInt(0, invalidSigs.length)] = RandomUtils.nextBytes(Key.Signature.LENGTH); // fuzz
        assertFalse(Native.verifyBatch(msgs, invalidSigs, pks));

        byte[][] invalidMsgs = Arrays.copyOf(pks, pks.length);
        invalidMsgs[RandomUtils.nextInt(0, invalidMsgs.length)] = RandomUtils.nextBytes(msgs[0].length); // fuzz
        assertFalse(Native.verifyBatch(invalidMsgs, sigs, pks));

        byte[][] invalidPks = Arrays.copyOf(pks, pks.length);
        invalidPks[RandomUtils.nextInt(0, invalidPks.length)] = RandomUtils.nextBytes(Key.PUBLIC_KEY_LEN); // fuzz
        assertFalse(Native.verifyBatch(msgs, sigs, invalidPks));

        assertFalse(Native.verifyBatch(invalidMsgs, invalidSigs, invalidPks));
    }

    @Test
    public void testCompatibility() {
        assertArrayEquals(Native.h256(MESSAGE), Hash.h256(MESSAGE));
        assertArrayEquals(Native.h160(MESSAGE), Hash.h160(MESSAGE));

        Key key = new Key();
        Key.Signature sig = key.sign(MESSAGE);

        byte[] pk2 = Bytes.merge(key.sk.getSeed(), key.sk.getAbyte());
        byte[] sig2 = Native.sign(MESSAGE, pk2);

        assertArrayEquals(sig.getS(), sig2);
    }

    @Test
    public void benchmarkH256() {
        byte[] data = Bytes.random(512);
        int repeat = 20_000;

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Hash.h256(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.h256(data);
        }
        Instant end = Instant.now();
        logger.debug("H256 Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Hash.h256(data);
        }
        end = Instant.now();
        logger.debug("H256 Java: " + Duration.between(start, end).toMillis() + "ms");

        assertArrayEquals(Hash.h256(data), Native.h256(data));
    }

    @Test
    public void benchmarkH160() {
        byte[] data = Bytes.random(512); // typical input
        int repeat = 20_000;

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Hash.h160(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.h160(data);
        }
        Instant end = Instant.now();
        logger.debug("H160 Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Hash.h160(data);
        }
        end = Instant.now();
        logger.debug("H160 Java: " + Duration.between(start, end).toMillis() + "ms");

        assertArrayEquals(Hash.h256(data), Native.h256(data));
    }

    @Test
    public void benchmarkSign() {
        byte[] data = Bytes.random(512);
        int repeat = 5_000;

        Key key = new Key();

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.sign(data, PRIVATE_KEY);
            key.sign(data);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.sign(data, PRIVATE_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Sign Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            key.sign(data);
        }
        end = Instant.now();
        logger.debug("Sign Java: " + Duration.between(start, end).toMillis() + "ms");
    }

    @Test
    public void benchmarkVerify() {
        byte[] data = Bytes.random(512);
        int repeat = 5_000;

        Key key = new Key();
        Key.Signature sig = key.sign(data);

        byte[] sigNative = Native.sign(data, PRIVATE_KEY);

        // warm up
        for (int i = 0; i < repeat / 10; i++) {
            Native.verify(data, sigNative, PUBLIC_KEY);
            Key.verify(data, sig);
        }

        // native
        Instant start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Native.verify(data, sigNative, PUBLIC_KEY);
        }
        Instant end = Instant.now();
        logger.debug("Verify Native: " + Duration.between(start, end).toMillis() + "ms");

        // java (JIT)
        start = Instant.now();
        for (int i = 0; i < repeat; i++) {
            Key.verify(data, sig);
        }
        end = Instant.now();
        logger.debug("Verify Java: " + Duration.between(start, end).toMillis() + "ms");
    }
}
