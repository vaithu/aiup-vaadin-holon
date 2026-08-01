package com.example.crm.ui;

// FALLBACK: no Holon equivalent for AppLayout, DrawerToggle, SideNav, or @StyleSheet
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Application shell: collapsible side drawer on desktop, hamburger menu on mobile.
 * Every {@code @Route} view opts in via {@code layout = MainLayout.class}.
 *
 * <p>FALLBACK: no Holon equivalent for AppLayout / SideNav / DrawerToggle / @StyleSheet.</p>
 *
 * <p>CSS is loaded from {@code src/main/resources/META-INF/resources/themes/crm/styles.css}
 * as a static resource — the Vaadin 25 recommended approach (no {@code @Theme} / Vite bundle
 * needed).</p>
 */
// FALLBACK: no Holon equivalent for @StyleSheet CSS loading
@StyleSheet("context://themes/crm/styles.css")
public class MainLayout extends AppLayout {

    public MainLayout() {
        // FALLBACK: no Holon equivalent for AppLayout application shell
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("MiniCRM");
        title.addClassName("crm-app-title");

        SideNav nav = buildNav();
        Scroller scroller = new Scroller(nav);
        scroller.setClassName("crm-nav-scroller");

        addToDrawer(scroller);
        addToNavbar(toggle, title);
        setPrimarySection(Section.DRAWER);
    }

    private static SideNav buildNav() {
        // FALLBACK: no Holon equivalent for SideNav navigation component
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Customers", CustomerListView.class));
        nav.addItem(new SideNavItem("Contacts",  ContactListView.class));
        return nav;
    }
}
