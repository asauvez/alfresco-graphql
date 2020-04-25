package fr.smile.alfresco.graphql.helper;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.query.ContentDataQL;
import fr.smile.alfresco.graphql.query.DateQL;
import fr.smile.alfresco.graphql.query.NodeQL;

public enum AlfrescoDataType {
	
	TEXT (DataTypeDefinition.TEXT, 			ScalarType.String, ScalarType.String),
	ANY (DataTypeDefinition.ANY, 			ScalarType.String, ScalarType.String),
	ENCRYPTED (DataTypeDefinition.ENCRYPTED, 	ScalarType.String, ScalarType.String),
	MLTEXT (DataTypeDefinition.MLTEXT, 		ScalarType.String, ScalarType.String),
	INT (DataTypeDefinition.INT, 			ScalarType.Int, ScalarType.Int),
	LONG (DataTypeDefinition.LONG, 			ScalarType.Int, ScalarType.Int, o -> (int) o),
	FLOAT (DataTypeDefinition.FLOAT, 		ScalarType.Float, ScalarType.Float),
	DOUBLE (DataTypeDefinition.DOUBLE, 		ScalarType.Float, ScalarType.Float, o -> (float) o),
	DATE (DataTypeDefinition.DATE, 			ScalarType.Date, ScalarType.String, o -> new DateQL((Date) o)),
	DATETIME (DataTypeDefinition.DATETIME, 	ScalarType.Date, ScalarType.String, o -> new DateQL((Date) o)),
	BOOLEAN (DataTypeDefinition.BOOLEAN, 	ScalarType.Boolean, ScalarType.Boolean),
	QNAME (DataTypeDefinition.QNAME, 		ScalarType.String, ScalarType.String),
	CATEGORY (DataTypeDefinition.CATEGORY, 	ScalarType.String, ScalarType.String),
	CHILD_ASSOC_REF (DataTypeDefinition.CHILD_ASSOC_REF, ScalarType.String, ScalarType.String),
	ASSOC_REF (DataTypeDefinition.ASSOC_REF, ScalarType.String, ScalarType.String),
	PATH (DataTypeDefinition.PATH, 			ScalarType.String, ScalarType.String),
	LOCALE (DataTypeDefinition.LOCALE, 		ScalarType.String, ScalarType.String),
	PERIOD (DataTypeDefinition.PERIOD, 		ScalarType.String, ScalarType.String),

	NODE_REF (DataTypeDefinition.NODE_REF, 	ScalarType.Node, ScalarType.ID) {
		@Override public Object toGraphQl(NodeQL parent, QName property, Serializable o) {
			return new NodeQL(parent.getQueryContext(), (NodeRef) o);
		}
	},
	CONTENT (DataTypeDefinition.CONTENT, 	ScalarType.ContentData, ScalarType.String) {
		@Override public Object toGraphQl(NodeQL parent, QName property, Serializable o) {
			return new ContentDataQL(parent.getQueryContext(), parent.getNodeRefInternal(), property, (ContentData) o);
		}
	},

	;

	private static Map<QName, AlfrescoDataType> mapForAlfrescoDataType = new HashMap<>();
	
	private QName alfrescoType;
	private ScalarType scalarType;
	private ScalarType scalarInput;
	private Function<Serializable, Object> toGraphQl;

	AlfrescoDataType(QName alfrescoType, ScalarType scalarType, ScalarType scalarInput) {
		this(alfrescoType, scalarType, scalarInput, o -> o);
	}
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
	public Object toGraphQl(NodeQL parent, QName property, Serializable o) {
		return toGraphQl.apply(o); 
	}
}