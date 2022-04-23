package com.company.scanvas.shell;

import com.company.scanvas.Detective;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "opennms-scanvas", name = "testvas", description = "Ping OpenVAS gateway.")
@Service
public class TestCommand implements Action {

    @Reference
    private Detective detective;

    @Override
    public Object execute() {
        detective.testPost();
        return null;
    }
}
