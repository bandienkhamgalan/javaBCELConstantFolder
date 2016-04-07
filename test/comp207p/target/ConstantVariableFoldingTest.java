package comp207p.target;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test constant variable folding
 */
public class ConstantVariableFoldingTest {

    ConstantVariableFolding cvf = new ConstantVariableFolding();

    @Test
    public void testMethodOne(){
        assertEquals(3650, cvf.methodOne());
    }

    @Test
    public void testMethodTwo(){
        assertEquals(1.67, cvf.methodTwo(), 0.001);
    }

    @Test
    public void testMethodThree(){
        assertEquals(false, cvf.methodThree());
    }
    
    @Test
    public void testMethodFour(){
        assertEquals(true, cvf.methodFour());
    }
    
    // test int conversions
    @Test
    public void testIntMethod1(){
        assertEquals("Long", ((Object)cvf.intMethod1()).getClass().getSimpleName());
    }

    @Test
    public void testIntMethod2(){
        assertEquals("Float", ((Object)cvf.intMethod2()).getClass().getSimpleName());
    }

    @Test
    public void testIntMethod3(){
        assertEquals("Double", ((Object)cvf.intMethod3()).getClass().getSimpleName());
    }

    //test long conversion
    @Test
    public void testLongMethod1(){
        assertEquals("Integer", ((Object)cvf.longMethod1()).getClass().getSimpleName());
    }

    @Test
    public void testLongMethod2(){
        assertEquals("Float", ((Object)cvf.longMethod2()).getClass().getSimpleName());
    }

    @Test
    public void testLongMethod3(){
        assertEquals("Double", ((Object)cvf.longMethod3()).getClass().getSimpleName());
    }

}
