package org.openforis.collect.android.viewmodelmanager;


import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.android.IdGenerator;
import org.openforis.collect.android.util.persistence.ConnectionCallback;
import org.openforis.collect.android.util.persistence.Database;
import org.openforis.collect.android.util.persistence.PreparedStatementHelper;
import org.openforis.collect.android.util.persistence.ResultSetHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Wiell
 */
public class DataSourceNodeRepository implements NodeRepository {

    private static final String[] FIELDS = new String[]{
        "relevant", "status", "parent_id", "parent_entity_id", "definition_id",
        "survey_id", "record_id", "record_collection_name", "record_key_attribute", "node_type",
        "val_text",
        "val_date",
        "val_hour", "val_minute",
        "val_code_value", "val_code_qualifier", "val_code_label",
        "val_boolean",
        "val_int", "val_int_from", "val_int_to",
        "val_double", "val_double_from", "val_double_to",
        "val_x", "val_y", "val_srs", "val_altitude", "val_accuracy",
        "val_taxon_code", "val_taxon_scientific_name", "val_file",
        "created_on", "modified_on",
        "id"
    };

    private static final String FIELDS_SELECT = StringUtils.join(FIELDS, ", ");
    private static final String SELECT_BY_RECORD_ID_QUERY;
    private static final String SELECT_BY_SURVEY_ID_QUERY;
    private static final String INSERT_QUERY;
    private static final String UPDATE_QUERY;
    static {
        // SELECT
        SELECT_BY_RECORD_ID_QUERY = "SELECT " + FIELDS_SELECT + "\n" +
                " FROM ofc_view_model\n" +
                " WHERE record_id = ?";
        SELECT_BY_SURVEY_ID_QUERY =  "SELECT " + FIELDS_SELECT + "\n" +
                "FROM ofc_view_model\n" +
                "WHERE survey_id = ? AND (parent_id IS NULL OR record_key_attribute = ?)\n" +
                "ORDER BY id";

        // INSERT
        String[] questionMarksArr = new String[FIELDS.length];
        Arrays.fill(questionMarksArr, "?");
        String questionMarks = StringUtils.join(questionMarksArr, ", ");
        INSERT_QUERY = "INSERT INTO ofc_view_model(" + FIELDS_SELECT + ")\n" +
                " VALUES(" + questionMarks + ")";

        // UPDATE
        List<String> fieldsToUpdate = new ArrayList<String>(Arrays.asList(FIELDS));
        fieldsToUpdate.remove("id");
        Collection<String> fieldsUpdateArr = Collections2.transform(
                fieldsToUpdate, new Function<String, String>() {
                    @Override
                    public String apply(String field) {
                        return field + " = ?";
                    }
                });
        String fieldsUpdate = StringUtils.join(fieldsUpdateArr, ", ");
        UPDATE_QUERY = "UPDATE ofc_view_model\n" +
                "SET " + fieldsUpdate + "\n" +
                "WHERE id = ?";
    }

    private final Database database;

    public DataSourceNodeRepository(Database database) {
        this.database = database;
        IdGenerator.setLastId(lastId());
    }

    private int lastId() {
        return database.execute(new ConnectionCallback<Integer>() {
            public Integer execute(Connection connection) throws SQLException {
                ResultSet rs = connection.createStatement().executeQuery("SELECT MAX(id) FROM ofc_view_model");
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    public void insert(final List<NodeDto> nodes, final Map<Integer, StatusChange> statusChanges) {
        database.execute(new ConnectionCallback<Void>() {
            public Void execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(INSERT_QUERY);
                for (NodeDto node : nodes) {
                    bind(ps, node);
                    ps.addBatch();
                }
                ps.executeBatch();
                ps.close();
                updateStatusChanges(connection, statusChanges);
                return null;
            }
        });
    }

    public void removeAll(final List<Integer> ids, final Map<Integer, StatusChange> statusChanges) {
        database.execute(new ConnectionCallback<Void>() {
            public Void execute(Connection connection) throws SQLException {
                removeNodes(connection, ids);
                updateStatusChanges(connection, statusChanges);
                return null;
            }
        });
    }

    private void removeNodes(Connection connection, List<Integer> ids) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM ofc_view_model WHERE id = ?");
        for (int id : ids) {
            ps.setInt(1, id);
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    public void removeRecord(final int recordId) {
        database.execute(new ConnectionCallback<Void>() {
            public Void execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM ofc_view_model WHERE record_id = ?");
                ps.setInt(1, recordId);
                ps.executeUpdate();
                ps.close();
                return null;
            }
        });
    }

    public NodeDto.Collection recordNodes(final int recordId) {
        return database.execute(new ConnectionCallback<NodeDto.Collection>() {
            public NodeDto.Collection execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_RECORD_ID_QUERY);
                ps.setInt(1, recordId);
                ResultSet rs = ps.executeQuery();
                NodeDto.Collection collection = new NodeDto.Collection();
                while (rs.next())
                    collection.addNode(toNode(rs));
                rs.close();
                ps.close();
                return collection;
            }
        });
    }

    public void update(final NodeDto node, final Map<Integer, StatusChange> statusChanges) {
        database.execute(new ConnectionCallback<Void>() {
            public Void execute(Connection connection) throws SQLException {
                updateAttribute(connection, node);
                updateStatusChanges(connection, statusChanges);
                return null;
            }
        });
    }

    public void updateModifiedOn(final NodeDto node) {
        database.execute(new ConnectionCallback<Void>() {
            public Void execute(Connection connection) throws SQLException {
                updateModifiedOn(connection, node);
                return null;
            }
        });
    }

    private void updateStatusChanges(Connection connection, Map<Integer, StatusChange> statusChanges) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("" +
                "UPDATE ofc_view_model\n" +
                "SET relevant = ?, status = ?\n" +
                "WHERE id = ?");
        for (Map.Entry<Integer, StatusChange> statusChangeEntry : statusChanges.entrySet()) {
            int id = statusChangeEntry.getKey();
            StatusChange statusChange = statusChangeEntry.getValue();
            ps.setBoolean(1, statusChange.relevant);
            ps.setString(2, statusChange.status);
            ps.setInt(3, id);
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    private void updateAttribute(Connection connection, NodeDto node) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(UPDATE_QUERY);
        bind(ps, node);
        int rowsUpdated = ps.executeUpdate();
        if (rowsUpdated != 1)
            throw new IllegalStateException("Expected exactly one row to be updated. Was " + rowsUpdated);
        ps.close();
    }

    private void updateModifiedOn(Connection connection, NodeDto node) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("" +
                "UPDATE ofc_view_model\n" +
                "SET modified_on = ?\n" +
                "WHERE id = ?");
        PreparedStatementHelper psh = new PreparedStatementHelper(ps);
        psh.setTimestamp(node.modifiedOn);
        psh.setInt(node.id);
        int rowsUpdated = ps.executeUpdate();
        if (rowsUpdated != 1)
            throw new IllegalStateException("Expected exactly one row to be updated. Was " + rowsUpdated);
        ps.close();
    }

    public NodeDto.Collection surveyRecords(final int surveyId) {
        return database.execute(new ConnectionCallback<NodeDto.Collection>() {
            public NodeDto.Collection execute(Connection connection) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(SELECT_BY_SURVEY_ID_QUERY);
                ps.setInt(1, surveyId);
                ps.setInt(2, 1);
                ResultSet rs = ps.executeQuery();
                NodeDto.Collection collection = new NodeDto.Collection();
                while (rs.next()) {
                    NodeDto node = toNode(rs);
                    if (node.parentId != null)
                        node.parentId = node.recordId; // Put key attributes directly under record // TODO: Ugly!
                    collection.addNode(node);
                }
                rs.close();
                ps.close();
                return collection;
            }
        });
    }

    private NodeDto toNode(ResultSet rs) throws SQLException {
        NodeDto n = new NodeDto();
        ResultSetHelper helper = new ResultSetHelper(rs);
        n.id = rs.getInt("id");
        n.relevant = rs.getBoolean("relevant");
        n.status = rs.getString("status");
        n.parentId = helper.getInteger("parent_id");
        n.parentEntityId = helper.getInteger("parent_entity_id");
        n.definitionId = rs.getString("definition_id");
        n.surveyId = rs.getInt("survey_id");
        n.recordId = rs.getInt("record_id");
        n.recordCollectionName = rs.getString("record_collection_name");
        n.recordKeyAttribute = rs.getBoolean("record_key_attribute");
        n.type = NodeDto.Type.byId(rs.getInt("node_type"));
        n.text = rs.getString("val_text");
        Long dateMillis = helper.getLong("val_date");
        n.date = dateMillis == null ? null : new Date(dateMillis);
        n.hour = helper.getInteger("val_hour");
        n.minute = helper.getInteger("val_minute");
        n.codeValue = rs.getString("val_code_value");
        n.codeQualifier = rs.getString("val_code_qualifier");
        n.codeLabel = rs.getString("val_code_label");
        n.booleanValue = helper.getBoolean("val_boolean");
        n.intValue = helper.getInteger("val_int");
        n.intFrom = helper.getInteger("val_int_from");
        n.intTo = helper.getInteger("val_int_to");
        n.doubleValue = helper.getDouble("val_double");
        n.doubleFrom = helper.getDouble("val_double_from");
        n.doubleTo = helper.getDouble("val_double_to");
        n.x = helper.getDouble("val_x");
        n.y = helper.getDouble("val_y");
        n.srs = rs.getString("val_srs");
        n.altitude = helper.getDouble("val_altitude");
        n.accuracy = helper.getDouble("val_accuracy");
        n.taxonCode = rs.getString("val_taxon_code");
        n.taxonScientificName = rs.getString("val_taxon_scientific_name");
        String filePath = rs.getString("val_file");
        n.file = filePath == null ? null : new File(filePath);
        n.createdOn = helper.getTimestamp("created_on");
        n.modifiedOn = helper.getTimestamp("modified_on");
        return n;
    }

    private void bind(PreparedStatement ps, NodeDto node) throws SQLException {
        PreparedStatementHelper psh = new PreparedStatementHelper(ps);
        psh.setBoolean(node.relevant);
        psh.setString(node.status);
        psh.setIntOrNull(node.parentId);
        psh.setIntOrNull(node.parentEntityId);
        psh.setString(node.definitionId);
        psh.setInt(node.surveyId);
        psh.setInt(node.recordId);
        psh.setString(node.recordCollectionName);
        psh.setBoolean(node.recordKeyAttribute);
        psh.setInt(node.type.id);
        psh.setString(node.text);
        psh.setLongOrNull(node.date == null ? null : node.date.getTime());
        psh.setIntOrNull(node.hour);
        psh.setIntOrNull(node.minute);
        psh.setString(node.codeValue);
        psh.setString(node.codeQualifier);
        psh.setString(node.codeLabel);
        psh.setBooleanOrNull(node.booleanValue);
        psh.setIntOrNull(node.intValue);
        psh.setIntOrNull(node.intFrom);
        psh.setIntOrNull(node.intTo);
        psh.setDoubleOrNull(node.doubleValue);
        psh.setDoubleOrNull(node.doubleFrom);
        psh.setDoubleOrNull(node.doubleTo);
        psh.setDoubleOrNull(node.x);
        psh.setDoubleOrNull(node.y);
        psh.setStringOrNull(node.srs);
        psh.setDoubleOrNull(node.altitude);
        psh.setDoubleOrNull(node.accuracy);
        psh.setString(node.taxonCode);
        psh.setString(node.taxonScientificName);
        psh.setStringOrNull(node.file == null ? null : node.file.getAbsolutePath());
        psh.setTimestamp(node.createdOn);
        psh.setTimestamp(node.modifiedOn);
        psh.setInt(node.id);
    }

}
