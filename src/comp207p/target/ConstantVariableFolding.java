package comp207p.target;

public class ConstantVariableFolding
{
    public int methodOne(){
        int a = 62;
        int b = (a + 764) * 3;
        return b + 1234 - a;
    }

    public double methodTwo(){
        double i = 0.67;
        int j = 1;
        return i + j;
    }

    public boolean methodThree(){
        int x = 12345;
        int y = 54321;
        return x > y;
    }

    public boolean methodFour(){
        long x = 4835783423L;
        long y = 400000;
        long z = x + y;
        return x > y;
    }

    //test int conversions
    public long intMethod1(){
        int a = 8;
        long x = (long)a;
        return x;
    }

    public float intMethod2(){
        int a = 8;
        float x = (float)a;
        return x;
    }

    public double intMethod3(){
        int a = 8;
        double x = (double)a;
        return x;
    }

    //test long conversions
    public int longMethod1(){
        long a = 8;
        int x = (int)a;
        return x;
    }

    public float longMethod2(){
        long a = 8;
        float x = (float)a;
        return x;
    }

    public double longMethod3(){
        long a = 8;
        double x = (double)a;
        return x;
    }



}
