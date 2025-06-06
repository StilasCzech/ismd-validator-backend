package com.dia.utility;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Slf4j
public class DataTypeConverter {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
    };

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
    };

    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    private DataTypeConverter() {
    }

    /**
     * Add a property with appropriate datatype detection and conversion
     */
    public static void addTypedProperty(Resource subject, Property property, String value, String lang, Model model) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            Literal typedLiteral = createTypedLiteral(value, model, lang, property.getLocalName());
            subject.addProperty(property, typedLiteral);
            log.debug("Added property {} with value {} as {}",
                    property.getLocalName(), value, typedLiteral.getDatatypeURI());
        } catch (Exception e) {
            log.warn("Type detection failed for value '{}' on property '{}': {}. Using rdfs:Literal instead.",
                    value, property.getLocalName(), e.getMessage());
            if (lang != null && !lang.isEmpty()) {
                subject.addProperty(property, value, lang);
            } else {
                subject.addProperty(property, value);
            }
        }
    }

    /**
     * Creates a typed literal based on the value content
     */
    public static Literal createTypedLiteral(String value, Model model, String lang, String propertyName) {
        if (lang != null && !lang.isEmpty()) {
            return model.createLiteral(value, lang);
        }

        DetectionResult result = detectDataType(value, propertyName);

        if (result.getXsdType() != null) {
            return switch (result.getXsdType()) {
                case "xsd:boolean" -> {
                    boolean boolValue = "true".equalsIgnoreCase(value) ||
                            "ano".equalsIgnoreCase(value) ||
                            "yes".equalsIgnoreCase(value);
                    yield model.createTypedLiteral(boolValue);
                }
                case "xsd:integer" -> model.createTypedLiteral(Integer.parseInt(value), XSDDatatype.XSDinteger);
                case "xsd:double" -> model.createTypedLiteral(Double.parseDouble(value), XSDDatatype.XSDdouble);
                case "xsd:date" -> model.createTypedLiteral(value, XSDDatatype.XSDdate);
                case "xsd:time" -> model.createTypedLiteral(value, XSDDatatype.XSDtime);
                case "xsd:dateTime" -> model.createTypedLiteral(value, XSDDatatype.XSDdateTime);
                case "xsd:anyURI" -> model.createTypedLiteral(value, XSDDatatype.XSDanyURI);
                case "xsd:string" -> model.createTypedLiteral(value, XSDDatatype.XSDstring);
                default -> {
                    log.warn("Unrecognized XSD type: {}. Defaulting to rdfs:Literal", result.getXsdType());
                    yield model.createLiteral(value);
                }
            };
        }

        log.warn("Could not detect datatype for value '{}' of property '{}'. Defaulting to rdfs:Literal",
                value, propertyName);
        return model.createLiteral(value);
    }

    @Getter
    private static class DetectionResult {
        private final String xsdType;
        private final String message;

        public DetectionResult(String xsdType, String message) {
            this.xsdType = xsdType;
            this.message = message;
        }

    }

    private static DetectionResult detectDataType(String value, String propertyName) {
        if (isBooleanValue(value)) {
            return new DetectionResult("xsd:boolean", "Detected as boolean");
        }

        if (isUri(value)) {
            return new DetectionResult("xsd:anyURI", "Detected as URI");
        }

        if (isDate(value)) {
            return new DetectionResult("xsd:date", "Detected as date");
        }

        if (isTime(value)) {
            return new DetectionResult("xsd:time", "Detected as time");
        }

        if (isDateTime(value)) {
            return new DetectionResult("xsd:dateTime", "Detected as dateTime");
        }

        if (isInteger(value)) {
            return new DetectionResult("xsd:integer", "Detected as integer");
        }

        if (isDouble(value)) {
            return new DetectionResult("xsd:double", "Detected as double");
        }

        String inferredType = inferTypeFromPropertyName(propertyName);
        if (inferredType != null) {
            return new DetectionResult(inferredType, "Inferred from property name");
        }

        return new DetectionResult("xsd:string", "Default to string");
    }

    private static String inferTypeFromPropertyName(String propertyName) {
        if (propertyName == null) {
            return null;
        }

        String lowerName = propertyName.toLowerCase();

        if (lowerName.contains("datum") || lowerName.contains("date")) {
            return "xsd:date";
        }

        if (lowerName.contains("cas") || lowerName.contains("time")) {
            return "xsd:time";
        }

        if (lowerName.contains("url") || lowerName.contains("uri") ||
                lowerName.contains("odkaz") || lowerName.contains("link")) {
            return "xsd:anyURI";
        }

        if (lowerName.contains("boolean") || lowerName.contains("flag") ||
                lowerName.contains("indicator") || lowerName.contains("is") ||
                lowerName.contains("has")) {
            return "xsd:boolean";
        }

        if (lowerName.contains("count") || lowerName.contains("number") ||
                lowerName.contains("integer") || lowerName.contains("quantity")) {
            return "xsd:integer";
        }

        return null;
    }

    public static boolean isBooleanValue(String value) {
        return "true".equalsIgnoreCase(value) ||
                "false".equalsIgnoreCase(value) ||
                "ano".equalsIgnoreCase(value) ||
                "ne".equalsIgnoreCase(value) ||
                "yes".equalsIgnoreCase(value) ||
                "no".equalsIgnoreCase(value);
    }

    public static boolean isInteger(String value) {
        return INTEGER_PATTERN.matcher(value).matches();
    }

    public static boolean isDouble(String value) {
        return DOUBLE_PATTERN.matcher(value).matches();
    }

    public static boolean isUri(String value) {
        try {
            java.net.URI uri = new java.net.URI(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // continue to next check
            }
        }
        return false;
    }

    public static boolean isTime(String value) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                LocalTime.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // continue to next check
            }
        }
        return false;
    }

    public static boolean isDateTime(String value) {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                LocalDateTime.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // continue to next check
            }
        }
        return false;
    }
}
