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

        //test float conversions
    public int floatMethod1(){
        float a = 8;
        int x = (int)a;
        return x;
    }

    public long floatMethod2(){
        float a = 8;
        long x = (long)a;
        return x;
    }

    public double floatMethod3(){
        float a = 8;
        double x = (double)a;
        return x;
    }

    //test double conversions
    public int doubleMethod1(){
        double a = 8;
        int x = (int)a;
        return x;
    }

    public float doubleMethod2(){
        double a = 8;
        float x = (float)a;
        return x;
    }

    public long doubleMethod3(){
        double a = 8;
        long x = (long)a;
        return x;
    }

    //test additions

    public int arithmeticInt(){
        int a = 8;
        int b = 2;
        int c = 1;

        int x =  a + b;
        int y = x - c;
        int z = y * b;
        int ret = z/3;

        return ret;
    }

    public long arithmeticLong(){
        long a = 8;
        long b = 2;
        long c = 1;

        long x =  a + b;
        long y = x - c;
        long z = y * b;
        long ret = z/3;
    
        return ret;
    }

    public float arithmeticFloat(){
        float a = (float)7.5;
        float b = (float)2.5;
        float c = (float)3;

        float x =  a + b;
        float y = x - c;
        float z = y * 2;
        float ret = z/4;
        return ret;
    }

    public double arithmeticDouble(){
        double a = 7.5;
        double b = 2.5;
        double c = 3;

        double x =  a + b;
        double y = x - c;
        double z = y * 2;
        double ret = z/4;
        return ret;
    }

    public int arithmeticComplex(){
        int a = 7;
        int b = 2;
        int c = 1;

        int x = a + (3 * c) - 15;
        int y = ((x * 3 + 20) / 5) + 99;
        return y;
    }

    //test comparisons() and if statements -- int 
    public boolean comparison1(){
        int a = 15;
        int b = 1;
        int c = 1;
        int x;
        int y;

        if(a < b)
            x = 10;
        else
            x = 30;

        if(b == c)
            y = 30;
        else 
            y = 5;

        if(a > b && x >= y)
            return x == y;
        else 
            return x < y;

    }

       //test comparisons() and if statements -- double 
    public boolean comparison2(){
        double a = 15;
        double b = 1;
        double c = 1;
        double x;
        double y;

        if(a < b)
            x = 10;
        else
            x = 30;

        if(b == c)
            y = 30;
        else 
            y = 5;

        if(a > b && x >= y)
            return x == y;
        else 
            return x < y;

    }


    //test negations
    public boolean negation1(){
        int a = 3;
        int b = 4;
        int c = 5;
        int x;
        int y;

        if(a != b)
            x = 10;
        else
            x = 5;

        if(!(b < c))
            y = 30;
        else 
            y = 10;

        return !(x != y);

    }

}
