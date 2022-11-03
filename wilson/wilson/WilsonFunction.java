package wilson;

import java.util.List;

import wilson.Stmt.Function;

public class WilsonFunction implements WilsonCallable 
{
    private final Function declaration;
    private final Environment closure;
    WilsonFunction(Function declaration, Environment closure)
    {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) 
    {
        Environment local = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++)
        {
            local.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try
        {
            interpreter.executeBlock(declaration.body, local);
        }
        catch(Return returnValue)
        {
            return returnValue.value;
        }
        
        return null;
    }

    @Override
    public int arity() 
    {
        return declaration.params.size();
    }

    public String toString()
    {
        return "<fn " + declaration.name.lexeme + ">";
    }
    
}
