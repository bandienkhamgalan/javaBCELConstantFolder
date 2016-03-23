package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
/*
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.TargetLostException; */
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.util.InstructionFinder.CodeConstraint;

public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here

		Method[] methods = cgen.getMethods();
		Method[] optimizedMethods = new Method[methods.length];
		for( int index = 0 ; index < methods.length ; index++ ) {
			Method m = methods[index];
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);

			InstructionList il = mg.getInstructionList();
			InstructionFinder finder = new InstructionFinder(il);
			
			Iterator itr = finder.search("ldc ldc ArithmeticInstruction");
			while(itr.hasNext()) {
				InstructionHandle[] instructions = (InstructionHandle[])itr.next();

				// fold constants
				int foldedConstantIndex = -1;
				switch(instructions[2].getInstruction().getName()) {
					case "iadd":
						System.out.println("adding two constants");
						LDC operandAInstruction = (LDC)(instructions[0].getInstruction());
						int operandA = (int)operandAInstruction.getValue(cpgen);
						LDC operandBInstruction = (LDC)(instructions[1].getInstruction());
						int operandB = (int)operandBInstruction.getValue(cpgen);
						foldedConstantIndex = cpgen.addInteger(operandA + operandB);
						System.out.printf("%d + %d = %d\n", operandA, operandB, operandA + operandB);
						System.out.printf("added new constant at index %d\n", foldedConstantIndex);
						break;
				}

				// insert new stack push instruction
				il.insert(instructions[0], new LDC(foldedConstantIndex));

				// remove stack push instructions
				try {
					il.delete(instructions[0].getInstruction(), instructions[2].getInstruction());
				} catch(TargetLostException e) {

				}
			}

			mg.stripAttributes(true)
			optimizedMethods[index] = mg.getMethod();
		}

		this.gen.setMethods(optimizedMethods);
        this.gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
	}

	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}