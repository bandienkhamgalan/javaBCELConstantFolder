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

			
			while(true) {
				System.out.println("running while");
				InstructionList il = mg.getInstructionList();
				System.out.println(il);

				InstructionFinder finder = new InstructionFinder(il);
				Iterator itr = finder.search("ldc ldc ArithmeticInstruction");

				if(!itr.hasNext())
					break;

				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					// fold constants
					int foldedConstantIndex = -1;
					LDC operandAInstruction = (LDC)(instructions[0].getInstruction());
					Object operandA = operandAInstruction.getValue(cpgen);
					LDC operandBInstruction = (LDC)(instructions[1].getInstruction());
					Object operandB = operandBInstruction.getValue(cpgen);
					switch(instructions[2].getInstruction().getName()) {
						case "iadd":
							foldedConstantIndex = cpgen.addInteger((int)operandA + (int)operandB);
							break;
						case "fadd":
							foldedConstantIndex = cpgen.addFloat((float)operandA + (float)operandB);
							break;
						case "dadd":
							foldedConstantIndex = cpgen.addDouble((double)operandA + (double)operandB);
							break;
						case "ladd":
							foldedConstantIndex = cpgen.addLong((long)operandA + (long)operandB);
							break;
						case "isub":
							foldedConstantIndex = cpgen.addInteger((int)operandA - (int)operandB);
							break;
						case "fsub":
							foldedConstantIndex = cpgen.addFloat((float)operandA - (float)operandB);
							break;
						case "dsub":
							foldedConstantIndex = cpgen.addDouble((double)operandA - (double)operandB);
							break;
						case "lsub":
							foldedConstantIndex = cpgen.addLong((long)operandA - (long)operandB);
							break;
						case "imul":
							foldedConstantIndex = cpgen.addInteger((int)operandA * (int)operandB);
							break;
						case "fmul":
							foldedConstantIndex = cpgen.addFloat((float)operandA * (float)operandB);
							break;
						case "dmul":
							foldedConstantIndex = cpgen.addDouble((double)operandA * (double)operandB);
							break;
						case "lmul":
							foldedConstantIndex = cpgen.addLong((long)operandA * (long)operandB);
							break;
						case "idiv":
							foldedConstantIndex = cpgen.addInteger((int)operandA / (int)operandB);
							break;
						case "fdiv":
							foldedConstantIndex = cpgen.addFloat((float)operandA / (float)operandB);
							break;
						case "ddiv":
							foldedConstantIndex = cpgen.addDouble((double)operandA / (double)operandB);
							break;
						case "ldiv":
							foldedConstantIndex = cpgen.addLong((long)operandA / (long)operandB);
							break;
					}

					// insert new stack push instruction
					il.insert(instructions[0], new LDC(foldedConstantIndex));

					// remove stack push instructions
					try {
						for( InstructionHandle i : instructions )
							il.delete(i);
					} catch(TargetLostException e) {

					}
				}
			}

			mg.stripAttributes(true);
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