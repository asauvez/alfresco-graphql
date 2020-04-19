package fr.smile.alfresco.graphql.helper;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.query.DateQL;

public enum AlfrescoDataType {
	
	TEXT (DataTypeDefinition.TEXT, 			ScalarType.String, ScalarType.String, o -> o.toString()),
	ANY (DataTypeDefinition.ANY, 			ScalarType.String, ScalarType.String, o -> o.toString()),
	ENCRYPTED (DataTypeDefinition.ENCRYPTED, 	ScalarType.String, ScalarType.String, o -> o.toString()),
	MLTEXT (DataTypeDefinition.MLTEXT, 		ScalarType.String, ScalarType.String, o -> o.toString()),
	CONTENT (DataTypeDefinition.CONTENT, 	ScalarType.ContentData, ScalarType.String, o -> o),  // special case
	INT (DataTypeDefinition.INT, 			ScalarType.Int, ScalarType.Int, o -> o),
	LONG (DataTypeDefinition.LONG, 			ScalarType.Int, ScalarType.Int, o -> (int) o),
	FLOAT (DataTypeDefinition.FLOAT, 		ScalarType.Float, ScalarType.Float, o -> o),
	DOUBLE (DataTypeDefinition.DOUBLE, 		ScalarType.Float, ScalarType.Float, o -> (float) o),
	DATE (DataTypeDefinition.DATE, 			ScalarType.Date, ScalarType.String, o -> new DateQL((Date) o)),
	DATETIME (DataTypeDefinition.DATETIME, 	ScalarType.Date, ScalarType.String, o -> new DateQL((Date) o)),
	BOOLEAN (DataTypeDefinition.BOOLEAN, 	ScalarType.Boolean, ScalarType.Boolean, o -> o),
	QNAME (DataTypeDefinition.QNAME, 		ScalarType.String, ScalarType.String, o -> o.toString()),
	CATEGORY (DataTypeDefinition.CATEGORY, 	ScalarType.String, ScalarType.String, o -> o.toString()),
	NODE_REF (DataTypeDefinition.NODE_REF, 	ScalarType.ID, ScalarType.ID, o -> o.toString()),
	CHILD_ASSOC_REF (DataTypeDefinition.CHILD_ASSOC_REF, ScalarType.String, ScalarType.String, o -> o.toString()),
	ASSOC_REF (DataTypeDefinition.ASSOC_REF, ScalarType.String, ScalarType.String, o -> o.toString()),
	PATH (DataTypeDefinition.PATH, 			ScalarType.String, ScalarType.String, o -> o.toString()),
	LOCALE (DataTypeDefinition.LOCALE, 		ScalarType.String, ScalarType.String, o -> o.toString()),
	PERIOD (DataTypeDefinition.PERIOD, 		ScalarType.String, ScalarType.String, o -> o.toString()),

	;

	private static Map<QName, AlfrescoDataType> mapForAlfrescoDataType = new HashMap<>();
	
	private QName alfrescoType;
	private ScalarType scalarType;
	private ScalarType scalarInput;
	private Function<Serializable, Object> toGraphQl;

	AlfrescoDataType(QName alfrescoType, ScalarType scalarType, ScalarType scalarInput, Function<Serializable, Object> toGraphQl) {
		this.alfrescoType = alfrescoType;
		this.scalarType = scalarType;
		this.scalarInput = scalarInput;
		this.toGraphQl = toGraphQl;
	}
	
	public static AlfrescoDataType getForAlfrescoDataType(QName dataType) {
		if (mapForAlfrescoDataType.isEmpty()) {
			for (AlfrescoDataType type : values()) {
				mapForAlfrescoDataType.put(type.alfrescoType, type);
			}
		}
		return mapForAlfrescoDataType.getOrDefault(dataType, AlfrescoDataType.TEXT);
	}
	
	public ScalarType getScalarType() {
		return scalarType;
	}
	public ScalarType getScalarInput() {
		return scalarInput;
	}
	public Object toGraphQl(Serializable o) {
		return toGraphQl.apply(o); 
	}
}