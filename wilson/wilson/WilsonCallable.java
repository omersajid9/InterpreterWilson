package wilson;


import java.util.List;

public interface WilsonCallable{
    Object call(Interpreter interpreter, List<Object> arguments);
    int arity();


}
