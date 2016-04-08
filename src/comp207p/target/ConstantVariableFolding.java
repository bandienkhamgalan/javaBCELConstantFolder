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

    //test comparisons and if statements
    public boolean comparison3(){
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

        //test negations
    public boolean negation1(){
        int a = 3;
        double b = 4;

        int c = -a;
        double d = -b;
        

        int e = -(-(-c));
        double f = -(-(-d));

        int x;
        int y;

        if(a == e)
            x = 10;
        else
            x = 0;

        if(b == f)
            y = 10;
        else 
            y = 5;

        return (x == y);

    }

    public int whileLoop1(){
        int a = 3;
        int b = 1;
        double c = 6;

        int iterator = 0;

        while(iterator < 10){
            a = a + 1;
            c = c + 1;
            iterator++;
        };

        int d = a + (int)c;

        return d;
    }

    public int doLoop1(){
        int a = 1;
        float b = (float)20;
        int i = 0;

        do{
            a = a + 1;
            b = b - 1;
            a = a -1;
            i++;
        }while(i < 10);

        int j = a + (int)b;
        return -j;
    }

    public float forLoop1(){
        int a = 1;
        float b = (float)10;

        for(int i = 0; i < 10; i++){
            if(a > 5)
                a = 5;
            else
                a++;
            b--;
        }
        return (float)a + b;
    }

    public int nestedLooping(){
        int a = 1;
        int b = -(-a) + 1;
        int c = 10;

        for(int i = 10; i > 0; i--){
            if(a < 5){
                while(b < 5){
                    a = a + 1;
                    b = b + 1;
                }
                a--;
            }
            else{
                a = a + 2;
            }
            b = 1;
        }
        return a;
    }

    public String caseStatement(){
        int a = 1;
        int b = 2;
        int c = -(-a + -b);
        c = c + 1;
        String res;

        switch(c){
            case 1 : res = "one";
                    break;
            case 2 : res = "two";
                    break;
            case 3 : res = "three";
                    break;
            case 4 : res = "four";
                    break;
            default : res = "invalid";
                    break;
        }
        return res;
    }

    public int parameters(int a, boolean b, double d){
        if(b == true)
            a = a * 2;
        return a + (int)d;
    }

    private int auxilary(int i){
        return i + 20;
    }

    public int privateMethod(){
        int a = 20;
        int b = 10;
        int c = auxilary(a);
        int d = auxilary(b);

        return c + d;
    }

    private int i = 200;
    private float f = (float)10;
    private String s = "hello";

    public float instanceVariable(){
        i = 1;
        f = (float)2;
        if(s == "hello")
            return i + f;
        return 0;
    }

    //inner class test
    private class Tire {
        private double radius;
        public Tire(double radius) {
          this.radius = radius;
        }
        public double getRadius() {
          return radius;
        }
        public void changeRadius(double change){
            radius = radius + change;
        }
    }

    public double innerClass(){
        Tire tire = new Tire(200);
        tire.changeRadius(200);
        double radius = tire.getRadius();
        return radius;
    }
}



