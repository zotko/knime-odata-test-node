package org.knime.examples.odatatest;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import java.util.Arrays;

public class ODataTestNodeDialog extends DefaultNodeSettingsPane {

    private static final String[] AVAILABLE_COLUMNS = new String[] { "ID", "Name", "Description", "Price" };

    protected ODataTestNodeDialog() {
        super();

        // Create settings models
        final SettingsModelStringArray selectedColumnsSettings = ODataTestNodeModel.createSelectedColumnsSettingsModel();
        final SettingsModelBoolean applyTopNSettings = ODataTestNodeModel.createApplyTopNSettingsModel();
        final SettingsModelInteger topNValueSettings = ODataTestNodeModel.createTopNValueSettingsModel();

        // Configure dependencies
        topNValueSettings.setEnabled(applyTopNSettings.getBooleanValue());

        applyTopNSettings.addChangeListener(e ->
            topNValueSettings.setEnabled(applyTopNSettings.getBooleanValue()));

        // Column Selection
        addDialogComponent(new DialogComponentStringListSelection(
            selectedColumnsSettings, "Select columns:", Arrays.asList(AVAILABLE_COLUMNS), true, 5));

        // Result Limit
        addDialogComponent(new DialogComponentBoolean(
            applyTopNSettings, "Limit number of results"));
        addDialogComponent(new DialogComponentNumber(
            topNValueSettings, "Max records:", 1, 5));
    }
}