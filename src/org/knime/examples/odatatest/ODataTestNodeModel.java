package org.knime.examples.odatatest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ODataTestNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ODataTestNodeModel.class);
    
    // Settings keys
    private static final String KEY_SELECTED_COLUMNS = "selected_columns";
    private static final String KEY_APPLY_TOP_N = "apply_top_n";
    private static final String KEY_TOP_N_VALUE = "top_n_value";
    
    // Constants
    private static final String ODATA_URL = "https://services.odata.org/V4/OData/OData.svc/Products";
    
    // Default values
    private static final String[] DEFAULT_COLUMNS = new String[] { "ID", "Name", "Description", "Price" };
    private static final boolean DEFAULT_APPLY_TOP_N = false;
    private static final int DEFAULT_TOP_N_VALUE = 10;
    
    // Settings models
    private final SettingsModelStringArray m_selectedColumnsSettings = createSelectedColumnsSettingsModel();
    private final SettingsModelBoolean m_applyTopNSettings = createApplyTopNSettingsModel();
    private final SettingsModelInteger m_topNValueSettings = createTopNValueSettingsModel();

    protected ODataTestNodeModel() {
        super(0, 1);
        
        // Make dependent settings available only when parent setting is enabled
        m_topNValueSettings.setEnabled(m_applyTopNSettings.getBooleanValue());
        
        // Add change listeners
        m_applyTopNSettings.addChangeListener(e -> 
            m_topNValueSettings.setEnabled(m_applyTopNSettings.getBooleanValue()));
    }

    static SettingsModelStringArray createSelectedColumnsSettingsModel() {
        return new SettingsModelStringArray(KEY_SELECTED_COLUMNS, DEFAULT_COLUMNS);
    }
    
    static SettingsModelBoolean createApplyTopNSettingsModel() {
        return new SettingsModelBoolean(KEY_APPLY_TOP_N, DEFAULT_APPLY_TOP_N);
    }
    
    static SettingsModelInteger createTopNValueSettingsModel() {
        return new SettingsModelInteger(KEY_TOP_N_VALUE, DEFAULT_TOP_N_VALUE);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        LOGGER.info("Starting OData product retrieval");
        
        // Get settings values
        String[] selectedColumns = m_selectedColumnsSettings.getStringArrayValue();
        boolean applyTopN = m_applyTopNSettings.getBooleanValue();
        int topNValue = m_topNValueSettings.getIntValue();
        
        // Validate selected columns
        if (selectedColumns.length == 0) {
            throw new InvalidSettingsException("At least one column must be selected");
        }
        
        // Create output table spec based on selected columns
        DataTableSpec outputSpec = createOutputSpec(selectedColumns);
        
        // Create container for the output table
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        
        try {
            // Create OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            // Build URL with query parameters
            HttpUrl.Builder urlBuilder = HttpUrl.parse(ODATA_URL).newBuilder();
            
            // Add $select parameter if specific columns are selected
            if (selectedColumns.length > 0 && selectedColumns.length < DEFAULT_COLUMNS.length) {
                String selectParam = String.join(",", selectedColumns);
                urlBuilder.addQueryParameter("$select", selectParam);
            }
            
            // Add $top parameter if top N is enabled
            if (applyTopN) {
                urlBuilder.addQueryParameter("$top", String.valueOf(topNValue));
            }
            
            // Build request
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .header("Accept", "application/json")
                    .build();
            
            LOGGER.info("Sending OData request to: " + urlBuilder.build().toString());
            
            // Execute the request
            try (Response response = client.newCall(request).execute()) {
                // Check if the request was successful
                if (response.isSuccessful() && response.body() != null) {
                    // Parse JSON response
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray products = jsonResponse.getJSONArray("value");
                    
                    // Create rows for each product
                    int rowCounter = 0;
                    int totalProducts = products.length();
                    
                    for (int i = 0; i < totalProducts; i++) {
                        JSONObject product = products.getJSONObject(i);
                        
                        List<DataCell> cells = new ArrayList<>();
                        
                        // Extract selected product properties and add them as cells
                        for (String column : selectedColumns) {
                            switch (column) {
                                case "ID":
                                    cells.add(new IntCell(product.getInt("ID")));
                                    break;
                                case "Name":
                                    cells.add(new StringCell(product.getString("Name")));
                                    break;
                                case "Description":
                                    cells.add(new StringCell(product.getString("Description")));
                                    break;
                                case "Price":
                                    cells.add(new DoubleCell(product.getDouble("Price")));
                                    break;
                                default:
                                    cells.add(DataType.getMissingCell());
                            }
                        }
                        
                        // Add row to table
                        DataRow row = new DefaultRow("Row" + rowCounter, cells);
                        container.addRowToTable(row);
                        rowCounter++;
                        
                        // Check for cancellation
                        exec.checkCanceled();
                        
                        // Report progress
                        exec.setProgress((double) rowCounter / totalProducts, 
                                "Added row " + rowCounter + " of " + totalProducts);
                    }
                    
                    LOGGER.info("Successfully retrieved " + rowCounter + " products from OData service");
                } else {
                    throw new Exception("Failed to retrieve products. Status code: " + response.code());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving products from OData service: " + e.getMessage(), e);
            throw e;
        } finally {
            container.close();
        }
        
        return new BufferedDataTable[] { container.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Validate selected columns
        String[] selectedColumns = m_selectedColumnsSettings.getStringArrayValue();
        if (selectedColumns.length == 0) {
            throw new InvalidSettingsException("At least one column must be selected");
        }
        
        // Validate top N value
        int topN = m_topNValueSettings.getIntValue();
        if (topN <= 0) {
            throw new InvalidSettingsException("Top N value must be greater than 0");
        }

        return new DataTableSpec[] { createOutputSpec(selectedColumns) };
    }

    private DataTableSpec createOutputSpec(String[] selectedColumns) {
        List<String> columnNames = new ArrayList<>();
        List<DataType> columnTypes = new ArrayList<>();
        
        for (String column : selectedColumns) {
            columnNames.add(column);
            
            switch (column) {
                case "ID":
                    columnTypes.add(IntCell.TYPE);
                    break;
                case "Name":
                case "Description":
                    columnTypes.add(StringCell.TYPE);
                    break;
                case "Price":
                    columnTypes.add(DoubleCell.TYPE);
                    break;
                default:
                    columnTypes.add(StringCell.TYPE);
            }
        }
        
        return new DataTableSpec(
                columnNames.toArray(new String[0]), 
                columnTypes.toArray(new DataType[0]));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumnsSettings.saveSettingsTo(settings);
        m_applyTopNSettings.saveSettingsTo(settings);
        m_topNValueSettings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumnsSettings.loadSettingsFrom(settings);
        m_applyTopNSettings.loadSettingsFrom(settings);
        m_topNValueSettings.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumnsSettings.validateSettings(settings);
        m_applyTopNSettings.validateSettings(settings);
        m_topNValueSettings.validateSettings(settings);
    }

    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        // Not required
    }

    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        // Not required
    }

    @Override
    protected void reset() {
        // Not required
    }
}