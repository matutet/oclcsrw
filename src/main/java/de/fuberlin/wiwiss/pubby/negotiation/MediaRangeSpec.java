package de.fuberlin.wiwiss.pubby.negotiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MediaRangeSpec {
    protected static Log log=LogFactory.getLog(MediaRangeSpec.class.getName());
	private final static Pattern TOKEN_PATTERN;
	private final static Pattern PARAMETER_PATTERN;
	private final static Pattern MEDIA_RANGE_PATTERN;
	private final static Pattern Q_VALUE_PATTERN;
	static {
		// See RFC 2616, section 2.2
		String token = "[\\x20-\\x7E&&[^()<>@,;:\\\"/\\[\\]?={} ]]+";
		String quotedString = "\"((?:[\\x20-\\x7E\\n\\r\\t&&[^\"\\\\]]|\\\\[\\x00-\\x7F])*)\"";
		// See RFC 2616, section 3.6
		String parameter = ";\\s*(?!q\\s*=)(" + token + ")=(?:(" + token + ")|" + quotedString + ")";
		// See RFC 2616, section 3.9
		String qualityValue = "(?:0(?:\\.\\d{0,3})?|1(?:\\.0{0,3})?)";
		// See RFC 2616, sections 14.1 
		String quality = ";\\s*q\\s*=\\s*([^;,]*)";
		// See RFC 2616, section 3.7
		String regex = "(" + token + ")/(" + token + ")" + 
				"((?:\\s*" + parameter + ")*)" +
				"(?:\\s*" + quality + ")?" +
				"((?:\\s*" + parameter + ")*)";
		TOKEN_PATTERN = Pattern.compile(token);
		PARAMETER_PATTERN = Pattern.compile(parameter);
		MEDIA_RANGE_PATTERN = Pattern.compile(regex);
		Q_VALUE_PATTERN = Pattern.compile(qualityValue);
	}

	/**
	 * Parses a media type from a string such as <tt>text/html;charset=utf-8;q=0.9</tt>.
	 */
	public static MediaRangeSpec parseType(String mediaType) {
		MediaRangeSpec m = parseRange(mediaType);
		if (m == null || m.isWildcardType() || m.isWildcardSubtype()) {
			return null;
		}
		return m;
	}

	/**
	 * Parses a media range from a string such as <tt>text/*;charset=utf-8;q=0.9</tt>.
	 * Unlike simple media types, media ranges may include wildcards.
	 */
	public static MediaRangeSpec parseRange(String mediaRange) {
		Matcher m = MEDIA_RANGE_PATTERN.matcher(mediaRange);
		if (!m.matches()) {
			return null;
		}
		String type = m.group(1).toLowerCase();
		String subtype = m.group(2).toLowerCase();
		String unparsedParameters = m.group(3);
		String qValue = m.group(7);
		m = PARAMETER_PATTERN.matcher(unparsedParameters);
		if ("*".equals(type) && !"*".equals(subtype)) {
			return null;
		}
		List<String> parameterNames = new ArrayList<String>();
		List<String> parameterValues = new ArrayList<String>();
		while (m.find()) {
			parameterNames.add(m.group(1).toLowerCase());
			parameterValues.add((m.group(3) == null) ? m.group(2) : unescape(m.group(3)));
		}
		double quality = 1.0;		
		if (qValue != null && Q_VALUE_PATTERN.matcher(qValue).matches()) {
			try {
				quality = Double.parseDouble(qValue);
			} catch (NumberFormatException ex) {
				// quality stays at default value
			}
		}
		return new MediaRangeSpec(type, subtype, parameterNames, parameterValues, quality);
	}
	
	/**
	 * Parses an HTTP Accept header into a List of MediaRangeSpecs
	 * @return A List of MediaRangeSpecs 
	 */
	public static List<MediaRangeSpec> parseAccept(String s) {
		List<MediaRangeSpec> result = new ArrayList<MediaRangeSpec>();
		Matcher m = MEDIA_RANGE_PATTERN.matcher(s);
                MediaRangeSpec pr;
                String group;
		while (m.find()) {
                    group = m.group();
                    pr = parseRange(group);
                    if(pr==null) {
                        log.error("null MediaRangeSpec generated from accept="+s+", group="+group);
                    }
			result.add(pr);
		}
		return result;
	}
	
	private static String unescape(String s) {
		return s.replaceAll("\\\\(.)", "$1");
	}
	
	private static String escape(String s) {
		return s.replaceAll("[\\\\\"]", "\\\\$0");
	}
	
	private final String type;
	private final String subtype;
	private final List<String> parameterNames;
	private final List<String> parameterValues;
	private final String mediaType;
	private       double quality;

	private MediaRangeSpec(String type, String subtype, 
			List<String> parameterNames, List<String> parameterValues,
			double quality) {
		this.type = type;
		this.subtype = subtype;
		this.parameterNames = Collections.unmodifiableList(parameterNames);
		this.parameterValues = parameterValues;
		this.mediaType = buildMediaType();
		this.quality = quality;
	}
	
	private String buildMediaType() {
		StringBuilder result = new StringBuilder();
		result.append(type);
		result.append("/");
		result.append(subtype);
		for (int i = 0; i < parameterNames.size(); i++) {
			result.append(";");
			result.append(parameterNames.get(i));
			result.append("=");
			String value = parameterValues.get(i);
			if (TOKEN_PATTERN.matcher(value).matches()) {
				result.append(value);
			} else {
				result.append("\"");
				result.append(escape(value));
				result.append("\"");
			}
		}
		return result.toString();
	}
	
	public String getType() {
		return type;
	}
	
	public String getSubtype() {
		return subtype;
	}
	
	public String getMediaType() {
		return mediaType;
	}
	
	public List<String> getParameterNames() {
		return parameterNames;
	}
	
	public String getParameter(String parameterName) {
		for (int i = 0; i < parameterNames.size(); i++) {
			if (parameterNames.get(i).equals(parameterName.toLowerCase())) {
				return parameterValues.get(i);
			}
		}
		return null;
	}
	
	public boolean isWildcardType() {
		return "*".equals(type);
	}
	
	public boolean isWildcardSubtype() {
		return !isWildcardType() && "*".equals(subtype);
	}
	
	public double getQuality() {
		return quality;
	}

    public void setQuality(Double value) {
        this.quality=value;
    }

    public int getPrecedence(MediaRangeSpec range) {
        if (range.isWildcardType()) return 1;
        if (!range.type.equals(type)) return 0;
        if (range.isWildcardSubtype()) return 2;
        if (!range.subtype.equals(subtype)) return 0;
        if (range.getParameterNames().isEmpty()) return 3;
        int result = 3;
        for (int i = 0; i < range.getParameterNames().size(); i++) {
            String name = range.getParameterNames().get(i);
            String value = range.getParameter(name);
            if (!value.equals(getParameter(name))) return 0;
            result++;
        }
        return result;
	}
	
	public MediaRangeSpec getBestMatch(List<MediaRangeSpec> mediaRanges) {
            MediaRangeSpec result = null;
            int bestPrecedence = 0;
            Iterator<MediaRangeSpec> it = mediaRanges.iterator();
            while (it.hasNext()) {
                MediaRangeSpec range = it.next();
                if (getPrecedence(range) > bestPrecedence) {
                    bestPrecedence = getPrecedence(range);
                    result = range;
                }
            }
            return result;
	}
	
    @Override
	public String toString() {
		return mediaType + ";q=" + quality;
	}
}