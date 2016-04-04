package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Arrays;

import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;
/*
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

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

	private void simpleFolding() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		Method[] methods = cgen.getMethods();
		Method[] optimizedMethods = new Method[methods.length];
		for( int index = 0 ; index < methods.length ; index++ ) {
			Method m = methods[index];
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
			InstructionList il = mg.getInstructionList();

			while(true) {
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

	private void constantVariableFolding() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();
		System.out.printf("CLASS %s\n", cgen.getClassName());

		Method[] methods = cgen.getMethods();
		Method[] optimizedMethods = new Method[methods.length];
		for( int index = 0 ; index < methods.length ; index++ ) {
			Method m = methods[index];
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
			InstructionList il = mg.getInstructionList();

			/* System.out.printf("METHOD %s\n", mg.getName());
			System.out.println("__before__");
			System.out.println(il); */

			HashMap constants = new HashMap<Integer, Number>();	// map from variable indices to values

			boolean performedOptimization;
			do {
				performedOptimization = false;

				/* BUILD/UPDATE CONSTANT VARIABLES DICTIONARY */
				InstructionFinder finder = new InstructionFinder(il);
				Iterator itr = finder.search("PushInstruction StoreInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					PushInstruction pushInstruction = (PushInstruction)instructions[0].getInstruction();
					StoreInstruction storeInstruction = (StoreInstruction)instructions[1].getInstruction();

					class VariableFinder implements CodeConstraint {
						public boolean checkCode(InstructionHandle[] match) {
							Instruction instruction = match[0].getInstruction();
							if(instruction instanceof IINC) {
								IINC incrementInstruction = (IINC)instruction;
								return incrementInstruction.getIndex() == storeInstruction.getIndex();
							} else if(instruction instanceof StoreInstruction) {
								StoreInstruction storeInstructionChecking = (StoreInstruction)instruction;
								return storeInstructionChecking.getIndex() == storeInstruction.getIndex();
							} else {
								return false;
							}
						}
					}

					Iterator storeInstructionIterator = finder.search("StoreInstruction | iinc", new VariableFinder());

					int counter = 0;
					while(storeInstructionIterator.hasNext()) {
						storeInstructionIterator.next();
						counter++;
					}

					if(counter == 1) {
						// found a constant variable, add to dictionary
						if(pushInstruction instanceof ConstantPushInstruction) {
							ConstantPushInstruction constantPushInstruction = (ConstantPushInstruction)pushInstruction;
							constants.put(storeInstruction.getIndex(), constantPushInstruction.getValue());
						} else if(pushInstruction instanceof LoadInstruction) {
							LoadInstruction loadInstruction = (LoadInstruction)pushInstruction;
							int variableIndex = loadInstruction.getIndex();
							if(constants.containsKey(variableIndex))
								constants.put(storeInstruction.getIndex(), constants.get(variableIndex));
						} else if(pushInstruction instanceof LDC) {
							System.out.println("ldc constant");
							LDC loadConstantInstruction = (LDC)pushInstruction;
							System.out.println(cpgen);
							System.out.println(loadConstantInstruction.getIndex());
							System.out.println(loadConstantInstruction.getType(cpgen));
							constants.put(storeInstruction.getIndex(), loadConstantInstruction.getValue(cpgen));
						} else if(pushInstruction instanceof LDC2_W) {
							System.out.println("ldc2_w constant");
							LDC2_W loadConstantInstruction = (LDC2_W)pushInstruction;
							constants.put(storeInstruction.getIndex(), loadConstantInstruction.getValue(cpgen));
						}
					}
				}

				// PERFORM CONVERSION
				itr = finder.search("PushInstruction ConversionInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();
					PushInstruction pushInstruction = (PushInstruction)instructions[0].getInstruction();
					ConversionInstruction conversionInstruction = (ConversionInstruction)instructions[1].getInstruction();
					Object operand = new Object();
					boolean isConstantFolding = true;

					if(pushInstruction instanceof LDC) {
						LDC loadConstantInstruction = (LDC)pushInstruction;
						operand = loadConstantInstruction.getValue(cpgen);
					} else if(pushInstruction instanceof LDC2_W) {
						LDC2_W loadConstantInstruction = (LDC2_W)pushInstruction;
						operand = loadConstantInstruction.getValue(cpgen);
					} else if(pushInstruction instanceof ConstantPushInstruction) {
						ConstantPushInstruction constantPushInstruction = (ConstantPushInstruction)pushInstruction;
						operand = constantPushInstruction.getValue();
					} else if(pushInstruction instanceof LoadInstruction) {
						LoadInstruction loadInstruction = (LoadInstruction)pushInstruction;
						if(constants.containsKey(loadInstruction.getIndex())) {
							operand = constants.get(loadInstruction.getIndex());
						} else {
							isConstantFolding = false;
							break;
						}
					}

					if(!isConstantFolding)
						continue;

					Number realOperand = (Number)operand;
					int foldedConstantIndex = -1;
					Instruction foldedInstruction = null;
					switch(conversionInstruction.getName()) {
						case "d2f":
						case "i2f":
						case "l2f":
							foldedInstruction = new LDC(cpgen.addFloat(realOperand.floatValue()));
							break;
						case "i2d":
						case "l2d":
						case "f2d":
							foldedInstruction = new LDC2_W(cpgen.addDouble(realOperand.doubleValue()));
							break;
						case "d2l":
						case "f2l":
						case "i2l":
							foldedInstruction = new LDC2_W(cpgen.addLong(realOperand.longValue()));
							break;
						case "d2i":
						case "f2i":
						case "l2i":
							foldedInstruction = new LDC2_W(cpgen.addInteger(realOperand.intValue()));
							break;
					}

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;

						il.insert(instructions[0], foldedInstruction);

						// remove stack push instructions
						try {
							for( InstructionHandle i : instructions )
								il.delete(i);
						} catch(TargetLostException e) { }
					}
				}

				// PERFORM NEGATION

				/* todo */

				// PERFORM FOLDING
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction PushInstruction ArithmeticInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					/*// debug print
					for(InstructionHandle i : instructions )
						System.out.println(i); */

					Object[] operands = new Object[2];
					boolean isConstantFolding = true;
					for( int j = 0 ; j < 2 ; j++ ) {
						// iterate over two push instructions
						PushInstruction pushInstruction = (PushInstruction)instructions[j].getInstruction();
						if(pushInstruction instanceof LDC) {
							LDC loadConstantInstruction = (LDC)pushInstruction;
							operands[j] = loadConstantInstruction.getValue(cpgen);
						} else if(pushInstruction instanceof LDC2_W) {
							LDC2_W loadConstantInstruction = (LDC2_W)pushInstruction;
							operands[j] = loadConstantInstruction.getValue(cpgen);
						} else if(pushInstruction instanceof ConstantPushInstruction) {
							ConstantPushInstruction constantPushInstruction = (ConstantPushInstruction)pushInstruction;
							operands[j] = constantPushInstruction.getValue();
						} else if(pushInstruction instanceof LoadInstruction) {
							LoadInstruction loadInstruction = (LoadInstruction)pushInstruction;
							if(constants.containsKey(loadInstruction.getIndex())) {
								operands[j] = constants.get(loadInstruction.getIndex());
							} else {
								isConstantFolding = false;
								break;
							}
						}
					}

					if(!isConstantFolding)
						continue;

					Number operandA = (Number)operands[0];
					Number operandB = (Number)operands[1];

					Instruction foldedInstruction = null;
					int foldedConstantIndex = -1;
					switch(instructions[2].getInstruction().getName()) {
						case "iadd":
							foldedInstruction = new LDC(cpgen.addInteger(operandA.intValue() + operandB.intValue()));
							break;
						case "fadd":
							foldedInstruction = new LDC(cpgen.addFloat(operandA.floatValue() + operandB.floatValue()));
							break;
						case "dadd":
							foldedInstruction = new LDC2_W(cpgen.addDouble(operandA.doubleValue() + operandB.doubleValue()));
							break;
						case "ladd":
							foldedInstruction = new LDC2_W(cpgen.addLong(operandA.longValue() + operandB.longValue()));
							break;
						case "isub":
							foldedInstruction = new LDC(cpgen.addInteger(operandA.intValue() - operandB.intValue()));
							break;
						case "fsub":
							foldedInstruction = new LDC(cpgen.addFloat(operandA.floatValue() - operandB.floatValue()));
							break;
						case "dsub":
							foldedInstruction = new LDC2_W(cpgen.addDouble(operandA.doubleValue() - operandB.doubleValue()));
							break;
						case "lsub":
							foldedInstruction = new LDC2_W(cpgen.addLong(operandA.longValue() - operandB.longValue()));
							break;
						case "imul":
							foldedInstruction = new LDC(cpgen.addInteger(operandA.intValue() * operandB.intValue()));
							break;
						case "fmul":
							foldedInstruction = new LDC(cpgen.addFloat(operandA.floatValue() * operandB.floatValue()));
							break;
						case "dmul":
							foldedInstruction = new LDC2_W(cpgen.addDouble(operandA.doubleValue() * operandB.doubleValue()));
							break;
						case "lmul":
							foldedInstruction = new LDC2_W(cpgen.addLong(operandA.longValue() * operandB.longValue()));
							break;
						case "idiv":
							foldedInstruction = new LDC(cpgen.addInteger(operandA.intValue() / operandB.intValue()));
							break;
						case "fdiv":
							foldedInstruction = new LDC(cpgen.addFloat(operandA.floatValue() / operandB.floatValue()));
							break;
						case "ddiv":
							foldedInstruction = new LDC2_W(cpgen.addDouble(operandA.doubleValue() / operandB.doubleValue()));
							break;
						case "ldiv":
							foldedInstruction = new LDC2_W(cpgen.addLong(operandA.longValue() / operandB.longValue()));
							break;
					}

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;

						il.insert(instructions[0], foldedInstruction);

						// remove stack push instructions
						try {
							for( InstructionHandle i : instructions )
								il.delete(i);
						} catch(TargetLostException e) { }
					}
				}
			} while(performedOptimization);


			mg.stripAttributes(true);
			optimizedMethods[index] = mg.getMethod();

			/* System.out.println("__after__");
			System.out.println(il); */
		}

		this.gen.setMethods(optimizedMethods);
        this.gen.setConstantPool(cpgen);
		this.optimized = gen.getJavaClass();
	}

	public void optimize()
	{
		// Implement your optimization here

		simpleFolding();
		constantVariableFolding();
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
