package com.opencratesoftware.mcnotepad.structs;

import com.opencratesoftware.mcnotepad.utils.Utils;

public class Variable 
{
    @Override
    public String toString()
    {
        if (!Name.equals(""))
        {
            return Name + " " + Value;
        }
        else
        {
            return "";
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        Variable otherVar = (Variable)obj;
        
        if (otherVar.Name.equals(Name)) { return true; }

        return false;
    }

    public Variable(String inName, String inValue)
    {
        Name = inName;
        Value = inValue;
    }
    public String Name;
    public String Value;  
}
