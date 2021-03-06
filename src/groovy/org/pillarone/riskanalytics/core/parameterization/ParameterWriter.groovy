package org.pillarone.riskanalytics.core.parameterization

import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.util.IConfigObjectWriter

class ParameterWriter implements IConfigObjectWriter {


    void write(ConfigObject configObject, BufferedWriter writer) {
        writer.append(configObject.model.getPackage().toString()).append("\n\n")
        printConfigObject("", configObject, writer, false, -1)
        writer.flush()
    }

    void printConfigObject(prefix, ConfigObject configObject, BufferedWriter out, boolean writePrefix, iteration) {

        boolean prefixRequired = writePrefix && configObject.flatten().size() > 0
        if (prefixRequired) {
            writeTabs(iteration, out)
            out << "$prefix {\n"
        }
        ++iteration
        configObject.each {key, value ->
            def keyString = prefix ? "${prefix}.${key}" : "$key"
            if (value instanceof ConfigObject) {
                Object classifier = value.values().find {it instanceof IParameterObjectClassifier}
                def periodLevelReached = value.keySet().any {!(it instanceof Integer)}
                if (!classifier) {
                    printConfigObject(key, value, out, periodLevelReached, iteration)
                }
                else {
                    Map params = new HashMap(value)
                    params.remove("type")
                    writeLine(key, keyString, out, [getType: {-> classifier}, getParameters: {-> params}] as IParameterObject, iteration)
                }
            }
            else {
                writeLine(key, keyString, out, value, iteration)
            }
        }
        if (prefixRequired) {
            writeTabs(iteration - 1, out)
            out << "}\n"
        }
    }

    private def writeTabs(count, writer) {
        count.times {
            writer << '\t'
        }
    }

    protected def writeLine(key, keyString, BufferedWriter out, value, iteration = 0) {
        writeTabs(iteration - 1, out)

        if (keyString.contains('.')) {
            keyString = "${keyString.substring(0, keyString.lastIndexOf('.'))}[$key]"
        }
        out << keyString << "="
        if (value.getClass().isEnum()) {
            out << "${value.getClass().name}.$value" as Object
        }
        else if (value instanceof List && !(value instanceof Range)) {
            out << "["
            if (!value.empty) {
                out << "\"" + value.join("\",\"") + "\""
            }
            out << "]"
        }
        else {
            appendValue out, value
        }
        out.newLine()
    }

    private void appendValue(BufferedWriter out, IParameterObject value) {
        out << value.type.getConstructionString(value.parameters)
    }

    private void appendValue(BufferedWriter out, Class value) {
        out << "${value.name}" as Object
    }

    private void appendValue(BufferedWriter out, String value) {
        out << "'$value'"
    }

    private void appendValue(BufferedWriter out, GString value) {
        out << "\"$value\""
    }

    private void appendValue(BufferedWriter out, Boolean value) {
        out << value.toString()
    }

    private void appendValue(BufferedWriter out, DateTime value) {
        out << "new ${DateTime.name}(${value.year}, ${value.monthOfYear}, ${value.dayOfMonth}, 0, 0, 0, 0)"
    }

    private void appendValue(BufferedWriter out, Object value) {
        out << value.toString()
    }


}