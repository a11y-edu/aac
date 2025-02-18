package com.a11yEdu.a11y.VideoTrimmer.fix;

import org.mp4parser.AbstractBoxParser;
import org.mp4parser.ParsableBox;
import org.mp4parser.tools.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PropertyBoxParserImpl extends AbstractBoxParser {
    public static Properties BOX_MAP_CACHE = null;
    public Properties mapping;
    static String[] EMPTY_STRING_ARRAY = new String[0];
    Pattern constuctorPattern = Pattern.compile("(.*)\\((.*?)\\)");
    StringBuilder buildLookupStrings = new StringBuilder();
    ThreadLocal<String> clazzName = new ThreadLocal();
    ThreadLocal<String[]> param = new ThreadLocal();

    public PropertyBoxParserImpl(String... customProperties) {
        if (BOX_MAP_CACHE != null) {
            this.mapping = new Properties(BOX_MAP_CACHE);
        } else {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("isoparser2-default.properties");

            try {
                this.mapping = new Properties();

                try {
                    this.mapping.load(is);
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl == null) {
                        cl = ClassLoader.getSystemClassLoader();
                    }

                    Enumeration<URL> enumeration = cl.getResources("isoparser-custom.properties");

                    while (enumeration.hasMoreElements()) {
                        URL url = (URL) enumeration.nextElement();
                        Throwable var6 = null;
                        Object var7 = null;

                        try {
                            InputStream customIS = url.openStream();

                            try {
                                this.mapping.load(customIS);
                            } finally {
                                if (customIS != null) {
                                    customIS.close();
                                }

                            }
                        } catch (Throwable var29) {
                            if (var6 == null) {
                                var6 = var29;
                            } else if (var6 != var29) {
                                var6.addSuppressed(var29);
                            }

                            throw var6;
                        }
                    }

                    String[] var35 = customProperties;
                    int var34 = customProperties.length;

                    for (int var33 = 0; var33 < var34; ++var33) {
                        String customProperty = var35[var33];
                        this.mapping.load(this.getClass().getResourceAsStream(customProperty));
                    }

                    BOX_MAP_CACHE = this.mapping;
                } catch (Throwable var30) {
                    throw new RuntimeException(var30);
                }
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException var27) {
                    var27.printStackTrace();
                }

            }
        }

    }

    public PropertyBoxParserImpl(Properties mapping) {
        this.mapping = mapping;
    }

    public ParsableBox createBox(String type, byte[] userType, String parent) {
        this.invoke(type, userType, parent);
        String[] param = (String[]) this.param.get();

        try {
            Class<ParsableBox> clazz = (Class<ParsableBox>) Class.forName((String) this.clazzName.get());
            if (param.length > 0) {
                Class[] constructorArgsClazz = new Class[param.length];
                Object[] constructorArgs = new Object[param.length];

                for (int i = 0; i < param.length; ++i) {
                    if ("userType".equals(param[i])) {
                        constructorArgs[i] = userType;
                        constructorArgsClazz[i] = byte[].class;
                    } else if ("type".equals(param[i])) {
                        constructorArgs[i] = type;
                        constructorArgsClazz[i] = String.class;
                    } else {
                        if (!"parent".equals(param[i])) {
                            throw new InternalError("No such param: " + param[i]);
                        }

                        constructorArgs[i] = parent;
                        constructorArgsClazz[i] = String.class;
                    }
                }

                Constructor<ParsableBox> constructorObject = clazz.getConstructor(constructorArgsClazz);
                return (ParsableBox) constructorObject.newInstance(constructorArgs);
            } else {
                return (ParsableBox) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException var9) {
            throw new RuntimeException(var9);
        }
    }

    public void invoke(String type, byte[] userType, String parent) {
        String constructor;
        if (userType != null) {
            if (!"uuid".equals(type)) {
                throw new RuntimeException("we have a userType but no uuid box type. Something's wrong");
            }

            constructor = this.mapping.getProperty("uuid[" + Hex.encodeHex(userType).toUpperCase() + "]");
            if (constructor == null) {
                constructor = this.mapping.getProperty(parent + "-uuid[" + Hex.encodeHex(userType).toUpperCase() + "]");
            }

            if (constructor == null) {
                constructor = this.mapping.getProperty("uuid");
            }
        } else {
            constructor = this.mapping.getProperty(type);
            if (constructor == null) {
                String lookup = this.buildLookupStrings.append(parent).append('-').append(type).toString();
                this.buildLookupStrings.setLength(0);
                constructor = this.mapping.getProperty(lookup);
            }
        }

        if (constructor == null) {
            constructor = this.mapping.getProperty("default");
        }

        if (constructor == null) {
            throw new RuntimeException("No box object found for " + type);
        } else {
            if (!constructor.endsWith(")")) {
                this.param.set(EMPTY_STRING_ARRAY);
                this.clazzName.set(constructor);
            } else {
                Matcher m = this.constuctorPattern.matcher(constructor);
                boolean matches = m.matches();
                if (!matches) {
                    throw new RuntimeException("Cannot work with that constructor: " + constructor);
                }

                this.clazzName.set(m.group(1));
                if (m.group(2).length() == 0) {
                    this.param.set(EMPTY_STRING_ARRAY);
                } else {
                    this.param.set(m.group(2).length() > 0 ? m.group(2).split(",") : new String[0]);
                }
            }

        }
    }
}


