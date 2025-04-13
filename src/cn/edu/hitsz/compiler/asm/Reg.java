package cn.edu.hitsz.compiler.asm;

public enum Reg {
    t0,t1,t2,t3,t4,t5,t6,error;
    public static Reg fromNum(int num){
        switch (num){
            case 0 :return Reg.t0;
            case 1 :return Reg.t1;
            case 2 :return Reg.t2;
            case 3 :return Reg.t3;
            case 4 :return Reg.t4;
            case 5 :return Reg.t5;
            case 6: return Reg.t6;
            default:return Reg.error;
        }
    }
    public static Boolean isError(Reg reg){
        return (reg==Reg.error);
    }
}
