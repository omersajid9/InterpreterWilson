package wilson;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    final Environment enclosing;

    Environment()
    {
        this.enclosing = null;
    }

    Environment(Environment enclosing)
    {
        this.enclosing = enclosing;
    }



    void define(String name, Object value)
    {
        values.put(name, value);
    }
    

    Object get(Token name)
    {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
        if (this.enclosing != null) return enclosing.get(name);
        
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value)
    {
        if (values.containsKey(name.lexeme)) 
        {
            values.put(name.lexeme, value); 
            return;
        }
        if (this.enclosing !=  null) 
        {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "' assigned.");
    }

    

    public Object getAt(Integer distance, String lexeme) 
    {
        return ancestor(distance).values.get(lexeme);
    }

    Environment ancestor(Integer distance)
    {
        Environment environment = this;
        for (int i = 0; i < distance; i++) environment = environment.enclosing;
        return environment;
        
    }

    public void assignAt(Integer distance, Token name, Object value) 
    {
        ancestor(distance).values.put(name.lexeme, value);
    }

    


}
