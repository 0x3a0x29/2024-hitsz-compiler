package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.BMap;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private BMap<Reg,IRVariable> Regs=new BMap<Reg,IRVariable>();
    private BMap<Integer,IRVariable> Stacks=new BMap<Integer, IRVariable>();
    private List<Instruction> instructions=new ArrayList<Instruction>();
    private List<String> assembly=new ArrayList<String>();
    private Integer StackSize=0;
    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        Instruction instruction,instructionNew;
        InstructionKind kind;
        IRValue lhs,rhs,temp;
        int i=0,num=0;
        IRVariable tempVar;
        while(true){
            instruction=originInstructions.get(i);
            kind=instruction.getKind();
            if (kind.isBinary()){
                lhs=instruction.getLHS();
                rhs=instruction.getRHS();
                if(lhs.isImmediate()&&rhs.isImmediate()){
                    switch (kind){
                        case ADD:
                            num=((IRImmediate)lhs).getValue()+((IRImmediate)rhs).getValue();
                            break;
                        case SUB:
                            num=((IRImmediate)lhs).getValue()-((IRImmediate)rhs).getValue();
                            break;
                        case MUL:
                            num=((IRImmediate)lhs).getValue()*((IRImmediate)rhs).getValue();
                            break;
                    }
                    instructionNew=Instruction.createMov(instruction.getResult(),IRImmediate.of(num));
                    instructions.add(instructionNew);
                    i+=1;
                    continue;
                }
                if(kind==InstructionKind.ADD && lhs.isImmediate()){
                    instructionNew=Instruction.createAdd(instruction.getResult(),instruction.getRHS(), instruction.getLHS());
                    instructions.add(instructionNew);
                }else if(kind==InstructionKind.MUL && (lhs.isImmediate()||rhs.isImmediate())){
                    tempVar=IRVariable.temp();
                    if (lhs.isImmediate()) {temp=lhs;lhs=rhs;}
                    else temp=rhs;
                    instructionNew=Instruction.createMov(tempVar,temp);
                    instructions.add(instructionNew);
                    instructionNew=Instruction.createMul(instruction.getResult(),tempVar,lhs);
                    instructions.add(instructionNew);
                }else if(kind==InstructionKind.SUB && lhs.isImmediate()){
                    tempVar=IRVariable.temp();
                    instructionNew=Instruction.createMov(tempVar,lhs);
                    instructions.add(instructionNew);
                    instructionNew=Instruction.createSub(instruction.getResult(),tempVar,rhs);
                    instructions.add(instructionNew);
                }else{
                    instructions.add(instruction);
                }
                i+=1;
            }else{
                instructions.add(instruction);
                if(kind.isReturn()) break;
                i+=1;
            }
        }
    }


    private Reg lastReg(IRVariable irVariable,int place){
        int[] regLast=new int[7];
        int index=0,max=-1;
        for (int i=0;i<7;i++){
            IRVariable temp=Regs.getByKey(Reg.fromNum(i));
            regLast[i]=instructions.size();
            for (int j=place;j<instructions.size();j++) {
                Instruction ins = instructions.get(j);
                InstructionKind kind = ins.getKind();
                if (kind.isBinary()) {
                    if (ins.getLHS().equals(temp) || ins.getRHS().equals(temp)) {
                        regLast[i] = j;
                        break;
                    }
                } else if (kind.isReturn()) {
                    if (ins.getReturnValue().equals(temp)) {
                        regLast[i] = j;
                        break;
                    }
                } else if (kind.isUnary()) {
                    if (ins.getFrom().equals(temp)) {
                        regLast[i] = j;
                        break;
                    }
                }
            }
        }
        for (int i=0;i<7;i++){
            if (max<regLast[i]){
                index=i;
                max=regLast[i];
            }
        }
        return Reg.fromNum(index);
    }
    private Reg getReg(IRVariable irVariable,int place){
        if (Regs.containsValue(irVariable)){
            return Regs.getByValue(irVariable);
        }
        if (Regs.getSize()<7){
            for (int i=0;i<7;i++){
                Reg r=Reg.fromNum(i);
                if (!Regs.containsKey(r)){
                    Regs.replace(r,irVariable);
                    return r;
                }
            }
        }
        boolean flag=true;
        for (int i=0;i<7;i++){
            IRVariable temp=Regs.getByKey(Reg.fromNum(i));
            flag=true;
            for (int j=place+1;j<instructions.size();j++){
                Instruction ins=instructions.get(j);
                InstructionKind kind=ins.getKind();
                if (kind.isBinary()) {
                    if (ins.getLHS().equals(temp) || ins.getRHS().equals(temp)) {
                        flag = false;
                        break;
                    }
                }else if(kind.isReturn()){
                    if (ins.getReturnValue().equals(temp)){
                        flag = false;
                        break;
                    }
                }else if(kind.isUnary()){
                    if (ins.getFrom().equals(temp)){
                        flag = false;
                        break;
                    }
                }
            }
            if(flag){
                Regs.replace(Reg.fromNum(i),irVariable);
                return Reg.fromNum(i);
            }
        }
        return Reg.error;
    }
    private Reg getRegByValue(IRVariable irVariable){
        if (Regs.containsValue(irVariable))
            return Regs.getByValue(irVariable);
        return Reg.error;
    }
    private List<String> Reg2Stack(Reg reg){
        if (!Regs.containsKey(reg))
            throw new RuntimeException("出现错误");
        List<String> instructions=new ArrayList<String>();
        IRVariable irVariable=Regs.getByKey(reg);
        if (!Stacks.containsValue(irVariable)){
            Stacks.replace(StackSize,irVariable);
            instructions.add("\tsw "+reg+",0(sp)\t#save:"+irVariable.toString());
            instructions.add("\taddi sp,sp,-4\t#move the stack pointer");
            StackSize+=1;
        }else{
            Integer place=Stacks.getByValue(irVariable);
            Integer offset=4*(StackSize-place);
            instructions.add("\tsw "+reg+","+offset.toString()+"(sp)\t#save:"+irVariable.toString());
            Stacks.replace(place,irVariable);
        }
        return instructions;
    }
    private String Stack2Reg(IRVariable irVariable,Reg reg){
        if (!Stacks.containsValue(irVariable))
            throw new RuntimeException("出现错误");
        String instruction;
        Integer place=Stacks.getByValue(irVariable);
        Integer offset=4*(StackSize-place);
        instruction="\tlw "+reg+","+offset.toString()+"(sp)\t#load:"+irVariable.toString();
        Regs.replace(reg,irVariable);
        return instruction;
    }
    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        assembly.add(".text");
        for (int i=0;i<instructions.size();i++){
            Instruction instruction=instructions.get(i);
            InstructionKind kind=instruction.getKind();
            String assemblyTemp="";
            IRVariable tempVariable;
            Reg tempReg;
            switch (kind){
                case MOV:
                    if (instruction.getFrom().isImmediate()){
                        assemblyTemp=((Integer)((IRImmediate)instruction.getFrom()).getValue()).toString();
                        tempVariable=instruction.getResult();
                        tempReg=getReg(tempVariable,i);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.addAll(Reg2Stack(tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp="\tli "+tempReg+", "+assemblyTemp+
                                "\t\t#   "+instruction.toString();
                        assembly.add(assemblyTemp);
                    }else{
                        Reg firstReg;
                        tempVariable=(IRVariable) instruction.getFrom();
                        tempReg=getRegByValue(tempVariable);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.add(Stack2Reg(tempVariable,tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        firstReg=tempReg;
                        assemblyTemp=", "+tempReg.toString();
                        tempVariable=instruction.getResult();
                        tempReg=getReg(tempVariable,i);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.addAll(Reg2Stack(tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        if (firstReg.equals(tempReg)){
                            assemblyTemp="\t# Change the meaning:"+tempReg.toString()+" ,from: "+instruction.toString();
                        }else{
                            assemblyTemp="\tmv "+tempReg.toString()+assemblyTemp+
                                "\t\t#   "+instruction.toString();
                        }
                        assembly.add(assemblyTemp);
                    }
                    break;
                case RET:
                    if (instruction.getReturnValue().isImmediate()){
                        assemblyTemp = "\tli a0, " + ((IRImmediate) instruction.getReturnValue()).toString();
                    }else {
                        tempVariable=(IRVariable) instruction.getReturnValue();
                        tempReg=getRegByValue(tempVariable);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.add(Stack2Reg(tempVariable,tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp = "\tmv a0, " + tempReg;
                    }
                    assemblyTemp+="\t\t#   "+instruction.toString();
                    assembly.add(assemblyTemp);
                    break;
                case ADD:
                    if (instruction.getRHS().isImmediate()){
                        tempVariable=(IRVariable) instruction.getLHS();
                        tempReg=getRegByValue(tempVariable);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.add(Stack2Reg(tempVariable,tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp =tempReg+", "+
                                ((IRImmediate) instruction.getRHS()).toString();
                        tempVariable=instruction.getResult();
                        tempReg=getReg(tempVariable,i);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.addAll(Reg2Stack(tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp="\taddi " +tempReg+", "+assemblyTemp;
                    }else {
                        tempVariable=(IRVariable) instruction.getLHS();
                        tempReg=getRegByValue(tempVariable);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.add(Stack2Reg(tempVariable,tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp =tempReg+", ";
                        tempVariable=(IRVariable) instruction.getRHS();
                        tempReg=getRegByValue(tempVariable);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.add(Stack2Reg(tempVariable,tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp=assemblyTemp+tempReg;
                        tempVariable=instruction.getResult();
                        tempReg=getReg(tempVariable,i);
                        if (Reg.isError(tempReg)){
                            tempReg=lastReg(tempVariable,i);
                            assembly.addAll(Reg2Stack(tempReg));
                            Regs.replace(tempReg,tempVariable);
                        }
                        assemblyTemp="\tadd " +tempReg+", "+assemblyTemp;
                    }
                    assemblyTemp+="\t#   "+instruction.toString();
                    assembly.add(assemblyTemp);
                    break;
                case MUL:
                    tempVariable=(IRVariable) instruction.getLHS();
                    tempReg=getRegByValue(tempVariable);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.add(Stack2Reg(tempVariable,tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    tempVariable=(IRVariable) instruction.getRHS();
                    assemblyTemp =tempReg+", ";
                    tempReg=getRegByValue(tempVariable);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.add(Stack2Reg(tempVariable,tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    tempVariable=instruction.getResult();
                    assemblyTemp=assemblyTemp+tempReg;
                    tempReg=getReg(tempVariable,i);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.addAll(Reg2Stack(tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    assemblyTemp="\tmul " +tempReg+", "+assemblyTemp+
                            "\t#   "+instruction.toString();
                    assembly.add(assemblyTemp);
                    break;
                case SUB:
                    tempVariable=(IRVariable) instruction.getLHS();
                    tempReg=getRegByValue(tempVariable);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.add(Stack2Reg(tempVariable,tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    tempVariable=(IRVariable) instruction.getRHS();
                    assemblyTemp =tempReg+", ";
                    tempReg=getRegByValue(tempVariable);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.add(Stack2Reg(tempVariable,tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    assemblyTemp=assemblyTemp+tempReg;
                    tempVariable=instruction.getResult();
                    tempReg=getReg(tempVariable,i);
                    if (Reg.isError(tempReg)){
                        tempReg=lastReg(tempVariable,i);
                        assembly.addAll(Reg2Stack(tempReg));
                        Regs.replace(tempReg,tempVariable);
                    }
                    assemblyTemp="\tsub " +tempReg+", "+assemblyTemp+
                            "\t#   "+instruction.toString();
                    assembly.add(assemblyTemp);
                    break;
            }
        }
        if (StackSize!=0){
            Integer offset=StackSize*4;
            assembly.add("\taddi sp,sp,"+offset.toString()+"\t#  Restore the stack pointer");
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path,assembly);
    }
}

