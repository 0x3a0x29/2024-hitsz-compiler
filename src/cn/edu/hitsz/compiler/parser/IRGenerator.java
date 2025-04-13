package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private List<Instruction> instructions=new ArrayList<Instruction>();
    private Stack<IRValue> irValues=new Stack<IRValue>();
    private SymbolTable symbolTable;
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        String id=currentToken.getKindId();
        if (id.equals("IntConst")) {
            irValues.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else if (id.equals("id")) {
            irValues.push(IRVariable.named(currentToken.getText()));
        } else {
            irValues.push(null);
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        int index=production.index();
        switch (index){
            case 6://S->id=E
                IRValue E6=irValues.pop();
                irValues.pop();
                IRValue id6=irValues.pop();
                instructions.add(Instruction.createMov((IRVariable)id6,E6));
                irValues.push(null);
                break;
            case 7://S->return E
                IRValue E7=irValues.pop();
                irValues.pop();
                instructions.add(Instruction.createRet(E7));
                irValues.push(null);
                break;
            case 8://E->E+A
                IRValue A8=irValues.pop();
                irValues.pop();
                IRValue E8=irValues.pop();
                IRVariable temp8=IRVariable.temp();
                instructions.add(Instruction.createAdd(temp8,E8,A8));
                irValues.push(temp8);
                break;
            case 9://E->E-A
                IRValue A9=irValues.pop();
                irValues.pop();
                IRValue E9=irValues.pop();
                IRVariable temp9=IRVariable.temp();
                instructions.add(Instruction.createSub(temp9,E9,A9));
                irValues.push(temp9);
                break;
            case 11://A->A*B
                IRValue B11=irValues.pop();
                irValues.pop();
                IRValue A11=irValues.pop();
                IRVariable temp11=IRVariable.temp();
                instructions.add(Instruction.createMul(temp11,A11,B11));
                irValues.push(temp11);
                break;
            case 13://B->(E)
                irValues.pop();
                IRValue E12=irValues.pop();
                irValues.pop();
                irValues.push(E12);
                break;
            default:
                break;
        }
    }
    @Override
    public void whenAccept(Status currentStatus) {
        return;
    }
    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable=table;
    }
    public List<Instruction> getIR() {
        return instructions;
    }
    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}