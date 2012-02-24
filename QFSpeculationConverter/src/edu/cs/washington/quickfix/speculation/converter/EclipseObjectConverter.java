package edu.cs.washington.quickfix.speculation.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class EclipseObjectConverter
{
    public static final String PLUG_IN_ID = "edu.washington.cs.quickfix.speculation.converter";
    private static final Logger logger = Logger.getLogger(EclipseObjectConverter.class.getName());
    static
    {
        logger.setLevel(Level.INFO);
    }
    private final IProject original_;
    private final String debugPrefix_;

    public EclipseObjectConverter(IProject original, String debugPrefix)
    {
        original_ = original;
        debugPrefix_ = debugPrefix;
    }

    protected ICompilationUnit convert(ICompilationUnit shadowCompilationUnit)
    {
        IPath projectRelativePath = shadowCompilationUnit.getResource().getProjectRelativePath();
        return JavaCore.createCompilationUnitFrom(original_.getFile(projectRelativePath));
    }
    
    protected IProject getOriginalProject()
    {
        return original_;
    }

    protected Object getFieldValue(String name, Object instance, Class <? extends Object> clazz)
            throws NoSuchFieldException, IllegalAccessException
    {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    protected void checkFieldName(boolean result, String fieldName, Object fieldValue)
    {
        if (!result)
            logUnexpectedFieldType(fieldName, fieldValue);
    }

    private void logUnexpectedFieldType(String fieldName, Object fieldValue)
    {
        logger.severe(fieldName + " is of incorrect type. " + fieldName + ".class = " + fieldValue.getClass());
    }

    protected Object getFieldValue(String name, Object instance) throws NoSuchFieldException, IllegalAccessException
    {
        return getFieldValue(name, instance, instance.getClass());
    }

    protected void logWrongUsageOfReflection(Object object, Exception thrown)
    {
        if (thrown != null)
            logger.log(Level.SEVERE, "There is a mistake in the usage of reflection. " + debugPrefix_ + ".class = "
                    + object.getClass(), thrown);
    }

    protected void logEclipseAPIChange(Object object)
    {
        logger.severe("Eclipse API changed. Correct the reflection calls. " + debugPrefix_ + ".class = "
                + object.getClass());
    }
    
    protected void logEclipseAPIChange(Object object, Exception thrown)
    {
        logger.log(Level.SEVERE, "Eclipse API changed. Correct the reflection calls. " + debugPrefix_ + ".class = "
                + object.getClass(), thrown);
    }

    protected void logUnknownObjectType(Object object)
    {
        logger.severe("Unknown " + debugPrefix_ + " type = " + object.getClass());
    }

    protected <T> T constructObject(Class <T> clazz, Class <?> [] argsClasses, Object [] args)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException
    {
        Constructor <T> constructor = clazz.getDeclaredConstructor(argsClasses);
        constructor.setAccessible(true);
        T object = constructor.newInstance(args);
        return object;
    }

    protected <T> T constructObject(Class <T> clazz, Class <?> argClass, Object arg) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException
    {
        return constructObject(clazz, new Class <?> [] {argClass}, new Object [] {arg});
    }

    protected <T> T constructObject(Class <T> clazz) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return constructObject(clazz, new Class <?> [0], new Object [0]);
    }

    protected <T> T constructObject(Class <T> clazz, Object... args) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        @SuppressWarnings("unchecked") Class <? extends Object> [] argsClasses = new Class [args.length];
        for (int a = 0; a < args.length; a++)
            argsClasses[a] = args[a].getClass();
        return constructObject(clazz, argsClasses, args);
    }

    protected Class <?> getInnerClass(Class <?> superClass, String stringRepresentation)
    {
        Class <?> classes[] = superClass.getDeclaredClasses();
        for (Class <?> clazz: classes)
        {
            if (clazz.toString().equals(stringRepresentation))
                return clazz;
        }
        return null;
    }

    protected Object invokeMethod(Class <?> clazz, String methodName, Class <?> [] argClasses, Object [] args,
            Object instance) throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException
    {
        Method method = clazz.getDeclaredMethod(methodName, argClasses);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    protected Object invokeMethod(Class <?> clazz, String methodName, Class <?> argClass, Object arg, Object instance)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        return invokeMethod(clazz, methodName, new Class <?> [] {argClass}, new Object [] {arg}, instance);
    }

    protected Object invokeMethod(Class <?> clazz, String methodName, Object instance) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        return invokeMethod(clazz, methodName, new Class <?> [0], new Object [0], instance);
    }

    public CompilationUnit createCompilationUnitFrom(ICompilationUnit compilationUnit)
    {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(compilationUnit);
        // TODO Eclipse API says that this is extremely costly. Maybe pass this as an argument where needed later?
        parser.setResolveBindings(true);
        // TODO safe conversion?
        CompilationUnit ASTCompliationUnit = (CompilationUnit) parser.createAST(null);
        return ASTCompliationUnit;
    }
}
