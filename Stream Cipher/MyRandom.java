import java.util.Random;

public class MyRandom extends Random {
    private int i = 0;
    private int j = 0;
    private int count = 0;
    private int k;
    private int[] s = new int[256];


    public MyRandom(){
        this(System.currentTimeMillis());
    }

    public MyRandom(long seed){
        this.setSeed(seed);
    }

    public MyRandom(String seed){
        this.setSeed(seed);
    }

    protected int next(int bits){
        this.count += 1;
        this.i = (this.i+1) % 256;
        this.j = (this.j + this.s[this.i]) % 256;
        // swap this.i & this.j
        int temp = this.s[this.i];
        this.s[this.i] = this.s[this.j];
        this.s[this.j] =  temp;
        // create the key for this.is byte
        temp = (this.s[this.i] + this.s[this.j]) % 256;
        k = this.s[temp];
        
        return k;
    }

    public void setSeed(String seed){
        byte[] key_stream = seed.getBytes();
        int key_length = key_stream.length;
        for (int i=0; i<256; i++){
            this.s[i] = i;
        }
        int j = 0;
        int temp = 0;
        for (int i=0; i<256; i++){
            j = (j + this.s[i] + key_stream[i % key_length]) % 256;
            temp = this.s[i];
            this.s[i] = this.s[j];
            this.s[j] = temp;

        }
    }
}