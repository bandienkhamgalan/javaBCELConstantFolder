package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;

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

public class ConstantFolder {
	private static class VariableFinder implements CodeConstraint {
		private int variableIndex;
		private int localVariableInstructionIndex;

		public VariableFinder(int variableIndex) {
			this.variableIndex = variableIndex;
			this.localVariableInstructionIndex = 0;
		}

		public VariableFinder(int variableIndex, int localVariableInstructionIndex) {
			this.variableIndex = variableIndex;
			this.localVariableInstructionIndex = localVariableInstructionIndex;
		}

		public boolean checkCode(InstructionHandle[] match) {
			LocalVariableInstruction instruction = (LocalVariableInstruction)(match[localVariableInstructionIndex].getInstruction());
			return instruction.getIndex() == variableIndex;
		}
	}

	private static class Jump {
		protected int from, to;

		public Jump(int from, int to) {
			this.from = from;
			this.to = to;
		}

		public Jump(Jump copy) {
			this(copy.from, copy.to);
		}

		public int getFrom() {
			return from;
		}

		public int getTo() {
			return to;
		}

		public void setFrom(int value) {
			from = value;
		}

		public void setTo(int value) {
			to = value;
		}

		public boolean contains(int test) {
			int lo, hi;
			if(from < to) {
				lo = from;
				hi = to;
			} else {
				lo = to;
				hi = from;
			}

			return test >= lo && test <= hi;
		}

		public String toString() {
			return "Jump from " + from + " to " + to;
		}
	}

	private static class JumpManager {
		private ArrayList<Jump> jumps;
		private HashSet<Integer> origins;
		private HashSet<Integer> destinations;

		public JumpManager() {
			this.jumps = new ArrayList<Jump>();
			recomputeSets();
		}

		public void addJump(Jump toAdd) {
			if(toAdd instanceof Jump) {
				jumps.add(toAdd);
				origins.add(toAdd.getFrom());
				destinations.add(toAdd.getTo());
			}
		}

		public void removeJump(Jump toRemove) {
			if(jumps.remove(toRemove))
				recomputeSets();
		}

		private void recomputeSets() {
			origins = new HashSet<Integer>();
			destinations = new HashSet<Integer>();
			for(Jump jump : jumps) {
				origins.add(jump.getFrom());
				destinations.add(jump.getTo());
			}
		}

		public ArrayList<Jump> jumpsContaining(int index) {
			ArrayList<Jump> matches = new ArrayList<Jump>();
			for(Jump jump : jumps)
				if(jump.contains(index))
					matches.add(new Jump(jump));
			return matches;
		}

		public boolean destinationsContain(int position) {
			for(int index : destinations)
				if(index == position)
					return true;
			return false;
		}

		public boolean destinationsContain(int[] positions) {
			for(int position : positions)
				if(destinationsContain(position))
					return true;
			return false;
		}

		public String toString() {
			return jumps.toString();
		}
	}

	private static class Range extends Jump {
		public Range(int from, int to) {
			super(from < to ? from : to, from < to ? to : from);
		}

		public String toString() {
			return "Range from " + from + " to " + to;
		}

		public boolean contains(int test) {
			return test >= from && test < to;
		}
	}

	private static class Variable {
		private final int index;
		private final ArrayList<Range> constantRanges;
		private final ArrayList<Object> values;

		private Variable(int index) {
			this.index = index;
			this.constantRanges = new ArrayList<Range>();
			this.values = new ArrayList<Object>();
		}

		public Variable(int index, ArrayList<Range> constantRanges) {
			this(index);
			for(Range range : constantRanges) {
				this.constantRanges.add(range);
				this.values.add(null);
			}
		}

		public int getIndex() {
			return index;
		}

		public Object getValueAtPosition(int position) {
			for(int index = 0 ; index < constantRanges.size() ; index++) {
				Range range = constantRanges.get(index);
				if(range.contains(position))
					return values.get(index);
			}
			return null;
		}

		public void setValueAtPosition(Object value, int position) {
			for(int index = 0 ; index < constantRanges.size() ; index++) {
				Range range = constantRanges.get(index);
				if(range.contains(position))
					values.set(index, value);
			}
		}

		public String toString() {
			String toReturn = String.format("Var #%d with %d constant value sections%c\n", index, constantRanges.size(), constantRanges.size() > 0 ? ':' : ' ');
			for(int index = 0 ; index < constantRanges.size() ; index++) {
				Range range = constantRanges.get(index);
				Object value = values.get(index);
				toReturn += String.format("\tvalue %s in %s\n", value, range);
			}
			return toReturn;
		}
	}

	private static class VariableManager {
		private HashMap<Integer, Variable> variables;

		public VariableManager() {
			this.variables = new HashMap<Integer, Variable>();
		}

		public VariableManager(ArrayList<Variable> variables) {
			this();
			for(Variable var : variables)
				this.variables.put(var.getIndex(), var);
		}

		public void addVariable(Variable toAdd) {
			variables.put(toAdd.getIndex(), toAdd);
		}

		public void removeVariable(Variable toRemove) {
			variables.remove(toRemove.getIndex());
		}

		public Variable getVariable(int index) {
			return variables.get(index);
		}

		public String toString() {
			return variables.values().toString();
		}

		public Object variableValueAtPosition(int variableIndex, int position) {
			Variable variable = variables.get(variableIndex);
			if(variable instanceof Variable) {
				Object value = variable.getValueAtPosition(position);
				if(value instanceof Object) {
					return value;
				}
			}
			return null;
		}
	}

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

	/*
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
	} */

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

	private HashSet<Integer> getAllVariableIndices(InstructionList il) {
		HashSet<Integer> indices = new HashSet<Integer>();
		InstructionFinder finder = new InstructionFinder(il);
		Iterator itr = finder.search("StoreInstruction");
		while(itr.hasNext()) {
			InstructionHandle[] instructions = (InstructionHandle[])itr.next();
			StoreInstruction storeInstruction = (StoreInstruction)(instructions[0].getInstruction());
			indices.add(storeInstruction.getIndex());
		}
		return indices;
	}

	private void dynamicVariableFolding() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();
		System.out.printf("CLASS %s\n", cgen.getClassName());

		Method[] methods = cgen.getMethods();
		Method[] optimizedMethods = new Method[methods.length];
		for( int index = 0 ; index < methods.length ; index++ ) {
			Method m = methods[index];
			MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
			InstructionList il = mg.getInstructionList();

			System.out.printf("METHOD %s\n", mg.getName());
			System.out.println("__before__");
			System.out.println(il);

			JumpManager jumpManager = new JumpManager();
			HashSet<Integer> localVariableIndices;
			VariableManager variableManager = new VariableManager();
			/* FIND ALL GOTOs */
			InstructionFinder finder = new InstructionFinder(il);
			Iterator itr = finder.search("BranchInstruction");

			while(itr.hasNext()) {
				InstructionHandle[] instructions = (InstructionHandle[])itr.next();
				BranchInstruction branchInstruction = (BranchInstruction)instructions[0].getInstruction();

				if(branchInstruction instanceof Select) {
					Select selectInstruction = (Select)branchInstruction;
					for(int offset : selectInstruction.getIndices()) {
						jumpManager.addJump(new Jump(instructions[0].getPosition(), instructions[0].getPosition() + offset));
					}
				} else {
					jumpManager.addJump(new Jump(instructions[0].getPosition(), instructions[0].getPosition() + branchInstruction.getIndex()));
				}

			}

			/* GET VARIABLE INDICES */
			localVariableIndices = getAllVariableIndices(il);
			// System.out.printf("%d local variables: %s\n", localVariableIndices.size(), localVariableIndices);

			/* DETERMINE CONSTANT SECTIONS OF VARIABLE */
			for(int varIndex : localVariableIndices) {
				itr = finder.search("StoreInstruction | iinc", new VariableFinder(varIndex, 0));
				ArrayList<Integer> positions = new ArrayList<Integer>();
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();
					positions.add(instructions[0].getPosition());
				}

				// System.out.printf("\tlocal variable %d was changed at: %s\n", varIndex, positions);

				ArrayList<Range> ranges = new ArrayList<Range>();
				Range current = new Range(positions.get(0), positions.get(0));
				boolean shouldAdd = true;
				for(int changePosIndex = 1 ; changePosIndex < positions.size() && shouldAdd ; changePosIndex++) {
					int changePosition = positions.get(changePosIndex);
					current.setTo(changePosition);
					ArrayList<Jump> jumpsContaining = jumpManager.jumpsContaining(changePosition);
					if(jumpsContaining.size() > 0) {
						int lowestStart = changePosition;
						for(Jump jump : jumpsContaining) {
							if(jump.getFrom() < lowestStart)
								lowestStart = jump.getFrom();
							if(jump.getTo() < lowestStart)
								lowestStart = jump.getTo();
						}
						current.setTo(lowestStart);
						shouldAdd = false;
					}

					Range toAdd = new Range(current.getFrom(), current.getTo());
					if(toAdd.getTo() - 1 > toAdd.getFrom())
						ranges.add(toAdd);
					current.setFrom(changePosition);
				}
				if(shouldAdd) {
					current.to = il.getEnd().getPosition() + 1;
					ranges.add(new Range(current.from, current.to));
				}

				variableManager.addVariable(new Variable(varIndex, ranges));
			}
			// System.out.println(variableManager);

			boolean performedOptimization;
			do {
				performedOptimization = false;

				/* UPDATE CONSTANT VARIABLES VALUES */
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction StoreInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					if(jumpManager.destinationsContain(instructions[1].getPosition())) {
						System.out.println("Skipping 'PushInstruction StoreInstruction' match across jump destination");
						continue;
					}

					PushInstruction pushInstruction = (PushInstruction)instructions[0].getInstruction();
					StoreInstruction storeInstruction = (StoreInstruction)instructions[1].getInstruction();

					Variable variable = variableManager.getVariable(storeInstruction.getIndex());
					int loadPosition = instructions[0].getPosition();
					int storePosition = instructions[1].getPosition();

					// found a constant variable, add to dictionary
					if(pushInstruction instanceof ConstantPushInstruction) {
						ConstantPushInstruction constantPushInstruction = (ConstantPushInstruction)pushInstruction;
						variable.setValueAtPosition(constantPushInstruction.getValue(), storePosition);
					} else if(pushInstruction instanceof LoadInstruction) {
						LoadInstruction loadInstruction = (LoadInstruction)pushInstruction;
						int variableIndex = loadInstruction.getIndex();
						Object value = variableManager.variableValueAtPosition(variableIndex, loadPosition);
						if(value instanceof Object)
							variable.setValueAtPosition(value, storePosition);
					} else if(pushInstruction instanceof LDC) {
						LDC loadConstantInstruction = (LDC)pushInstruction;
						variable.setValueAtPosition(loadConstantInstruction.getValue(cpgen), storePosition);
					} else if(pushInstruction instanceof LDC2_W) {
						LDC2_W loadConstantInstruction = (LDC2_W)pushInstruction;
						variable.setValueAtPosition(loadConstantInstruction.getValue(cpgen), storePosition);
					}
				}
				// System.out.println(variableManager);

				/* PERFORM CONVERSIONS */
				itr = finder.search("PushInstruction ConversionInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					if(jumpManager.destinationsContain(instructions[1].getPosition())) {
						System.out.println("Skipping 'PushInstruction ConversionInstruction' match across jump destination");
						continue;
					}

					PushInstruction pushInstruction = (PushInstruction)instructions[0].getInstruction();
					ConversionInstruction conversionInstruction = (ConversionInstruction)instructions[1].getInstruction();
					Object operand = null;

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
						Object value = variableManager.variableValueAtPosition(loadInstruction.getIndex(), instructions[0].getPosition());
						if(value instanceof Object)
							operand = value;
					}

					if(operand == null)
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
							foldedInstruction = new LDC(cpgen.addInteger(realOperand.intValue()));
							break;
					}

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;
						instructions[0].setInstruction(foldedInstruction);

						// remove stack push instructions
						try {
							il.delete(instructions[1]);
						} catch(TargetLostException e) { }
					}
				}

				/* PERFORM NEGATIONS */
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction ArithmeticInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					if(jumpManager.destinationsContain(instructions[1].getPosition())) {
						System.out.println("Skipping 'PushInstruction ArithmeticInstruction' match across jump destination");
						continue;
					}

					PushInstruction pushInstruction = (PushInstruction)instructions[0].getInstruction();
					ArithmeticInstruction arithmeticInstruction = (ArithmeticInstruction)instructions[1].getInstruction();
					Object operand = null;

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
						Object value = variableManager.variableValueAtPosition(loadInstruction.getIndex(), instructions[0].getPosition());
						if(value instanceof Object)
							operand = value;
					}

					if(operand == null)
						continue;

					Number realOperand = (Number)operand;
					int foldedConstantIndex = -1;
					Instruction foldedInstruction = null;
					switch(arithmeticInstruction.getName()) {
						case "fneg":
							foldedInstruction = new LDC(cpgen.addFloat(-realOperand.floatValue()));
							break;
						case "dneg":
							foldedInstruction = new LDC2_W(cpgen.addDouble(-realOperand.doubleValue()));
							break;
						case "lneg":
							foldedInstruction = new LDC2_W(cpgen.addLong(-realOperand.longValue()));
							break;
						case "ineg":
							foldedInstruction = new LDC(cpgen.addInteger(-realOperand.intValue()));
							break;
					}

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;
						instructions[0].setInstruction(foldedInstruction);

						// remove stack push instructions
						try {
							il.delete(instructions[1]);
						} catch(TargetLostException e) { }
					}
				}

				/* PERFORM FOLDING */
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction PushInstruction ArithmeticInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					/* // debug print
					for(InstructionHandle i : instructions )
						System.out.println(i); */

					if(jumpManager.destinationsContain(new int[]{instructions[1].getPosition(), instructions[2].getPosition()})) {
						System.out.println("Skipping 'PushInstruction PushInstruction ArithmeticInstruction' match across jump destination");
						continue;
					}

					Object[] operands = new Object[]{null, null};
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
							Object value = variableManager.variableValueAtPosition(loadInstruction.getIndex(), instructions[j].getPosition());
							if(value instanceof Object)
								operands[j] = value;
						}
					}

					if(operands[0] == null || operands[1] == null)
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
						instructions[0].setInstruction(foldedInstruction);

						// remove stack push instructions
						try {
							il.delete(instructions[1]);
							il.delete(instructions[2]);
						} catch(TargetLostException e) { }
					}
				}

				/* PERFORM INTEGER IF FOLDING */
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction PushInstruction IfInstruction");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					/* // debug print
					for(InstructionHandle i : instructions )
						System.out.println(i); */

					if(jumpManager.destinationsContain(new int[]{instructions[1].getPosition(), instructions[2].getPosition()})) {
						System.out.println("Skipping 'PushInstruction PushInstruction IfInstruction' match across jump destination");
						continue;
					}

					Object[] operands = new Object[]{null, null};
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
							Object value = variableManager.variableValueAtPosition(loadInstruction.getIndex(), instructions[j].getPosition());
							if(value instanceof Object)
								operands[j] = value;
						}
					}

					if(operands[0] == null || operands[1] == null)
						continue;

					Number operandA = (Number)operands[0];
					Number operandB = (Number)operands[1];

					Instruction foldedInstruction = null;
					int foldedConstantIndex = -1;
					switch(instructions[2].getInstruction().getName()) {
						case "if_icmpeq":
							foldedInstruction = new ICONST(operandA.intValue() == operandB.intValue() ? 1 : 0);
							break;
						case "if_icmpge":
							foldedInstruction = new ICONST(operandA.intValue() >= operandB.intValue() ? 1 : 0);
							break;
						case "if_icmpgt":
							foldedInstruction = new ICONST(operandA.intValue() > operandB.intValue() ? 1 : 0);
							break;
						case "if_icmple":
							foldedInstruction = new ICONST(operandA.intValue() <= operandB.intValue() ? 1 : 0);
							break;
						case "if_icmplt":
							foldedInstruction = new ICONST(operandA.intValue() < operandB.intValue() ? 1 : 0);
							break;
						case "if_icmpne":
							foldedInstruction = new ICONST(operandA.intValue() != operandB.intValue() ? 1 : 0);
							break;
					}

					Instruction ifInstruction = instructions[2].getInstruction();
					BranchInstruction branchInstruction = (BranchInstruction)ifInstruction;
					InstructionHandle target = branchInstruction.getTarget();
					IFGT foldedIfInstruction = new IFGT(target);

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;
						instructions[0].setInstruction(foldedInstruction);

						try {
							il.delete(instructions[1]);
						} catch(TargetLostException e) { }

						il.append(instructions[0], foldedIfInstruction);

						// remove stack push instructions
						try {
							il.delete(instructions[2]);
						} catch(TargetLostException e) { }
					}
				}

				/* PERFORM CMP FOLDING */
				finder = new InstructionFinder(il);
				itr = finder.search("PushInstruction PushInstruction (DCMPG|DCMPL|FCMPG|FCMPL|LCMP)");
				while(itr.hasNext()) {
					InstructionHandle[] instructions = (InstructionHandle[])itr.next();

					/* // debug print
					for(InstructionHandle i : instructions )
						System.out.println(i); */

					if(jumpManager.destinationsContain(new int[]{instructions[1].getPosition(), instructions[2].getPosition()})) {
						System.out.println("Skipping 'PushInstruction PushInstruction (DCMPG|DCMPL|FCMPG|FCMPL|LCMP)' match across jump destination");
						continue;
					}

					Object[] operands = new Object[]{null, null};
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
							Object value = variableManager.variableValueAtPosition(loadInstruction.getIndex(), instructions[j].getPosition());
							if(value instanceof Object)
								operands[j] = value;
						}
					}

					if(operands[0] == null || operands[1] == null)
						continue;

					Number operandA = (Number)operands[0];
					Number operandB = (Number)operands[1];

					Instruction foldedInstruction = null;
					int foldedConstantIndex = -1;
					switch(instructions[2].getInstruction().getName()) {
						case "dcmpg":
							Double doubleOperandA = new Double(operandA.doubleValue());
							Double doubleOperandB = new Double(operandB.doubleValue());
							foldedInstruction = new ICONST(doubleOperandA.compareTo(doubleOperandB));
							break;
						case "dcmpl":
							Double doubleOperandA1 = new Double(operandA.doubleValue());
							Double doubleOperandB1 = new Double(operandB.doubleValue());
							foldedInstruction = new ICONST(-doubleOperandA1.compareTo(doubleOperandB1));
							break;
						case "fcmpg":
							Float floatOperandA = new Float(operandA.floatValue());
							Float floatOperandB = new Float(operandB.floatValue());
							foldedInstruction = new ICONST(floatOperandA.compareTo(floatOperandB));
							break;
						case "fcmpl":
							Float floatOperandA1 = new Float(operandA.floatValue());
							Float floatOperandB1 = new Float(operandB.floatValue());
							foldedInstruction = new ICONST(-floatOperandA1.compareTo(floatOperandB1));
							break;
						case "lcmp":
							Long longOperandA = new Long(operandA.longValue());
							Long longOperandB = new Long(operandB.longValue());
							foldedInstruction = new ICONST(longOperandA.compareTo(longOperandB));
							break;
					}

					// insert new stack push instruction
					if(foldedInstruction != null) {
						performedOptimization = true;
						instructions[0].setInstruction(foldedInstruction);

						// remove stack push instructions
						try {
							il.delete(instructions[1]);
							il.delete(instructions[2]);
						} catch(TargetLostException e) { }
					}
				}

			} while(performedOptimization);

			System.out.printf("METHOD %s\n", mg.getName());
			System.out.println(cpgen);
			System.out.println("__after__");
			System.out.println(il);

			mg.stripAttributes(true);
			mg.setMaxStack();
			optimizedMethods[index] = mg.getMethod();
		}

		this.gen.setMethods(optimizedMethods);
        this.gen.setConstantPool(cpgen);
        this.gen.setMajor(50);
		this.optimized = gen.getJavaClass();
	}

	public void optimize()
	{
		// Implement your optimization here

		// simpleFolding();
		// constantVariableFolding();
		dynamicVariableFolding();
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
