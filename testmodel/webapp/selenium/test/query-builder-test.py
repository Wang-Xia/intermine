# -*- coding: utf-8 -*-

import unittest
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.alert import Alert
from selenium.webdriver.support.ui import Select, WebDriverWait
from test.testmodeltestcase import TestModelTestCase as Super

EXPECTED_TYPES = ['Bank', 'Broke', 'Employment Period', 'Has Address',
    'Has Secretarys', 'Important Person', 'Random Interface', 'Range', 'Secretary', 'Thing', 'Types']

class QueryBuilderTestCase(Super):

    def setUp(self):
        Super.setUp(self)
        self.browser.get(self.base_url + '/customQuery.do')

    def test_on_right_page(self):
        self.assertIn('Custom query', self.browser.title)

    def test_browse_data_model(self):
        link = self.findLink("Browse data model")
        self.assertIsNotNone(link)
        link.click()
        help_text = self.elem('.body > p').text
        self.assertIn("browse the tree", help_text)
        for type_name in EXPECTED_TYPES:
            self.assertIsNotNone(self.findLink(type_name))

        self.findLink('Bank').click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)

    def start_query_from_select(self):
        cls = 'Employee'
        Select(self.elem("#queryClassSelector")).select_by_visible_text(cls)
        self.elem("#submitClassSelect").click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals(cls, self.elem('.typeSelected').text)
        self.elem('a[title="Show Employee in results"] > img.arrow').click()

    def test_query_tree(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Bank")
        self.elem("#submitClassSelect").click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        self.assertEquals('Name', self.elem('.attributeField').text)
        cc = self.browser.find_element_by_id('drag_Bank.corporateCustomers')
        self.assertEquals('Corporate Customers', cc.text)
        ds = self.browser.find_element_by_id('drag_Bank.debtors')
        self.assertEquals('Debtors', ds.text)
        # Add a view element.
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Show debt in results"] > img.arrow').click()
        self.assertEquals(1, len(self.browser.find_elements_by_class_name('viewpath')))
        self.elem('a[title="Show name in results"] > img.arrow').click()
        self.assertEquals(2, len(self.browser.find_elements_by_class_name('viewpath')))
        # Add a constraint
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem("#attribute8").clear()
        self.elem("#attribute8").send_keys("1000")
        self.elem("#attributeSubmit").click()
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)
        self.elem('a[title="Export this query as XML"]').click()
        expected_query = """
<query name="" model="testmodel" view="Bank.debtors.debt Bank.name" longDescription="" sortOrder="Bank.debtors.debt asc">
  <constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>
</query>
        """
        self.assertEquals(expected_query.strip(), self.elem('body').text)

    def test_build_query(self):
        Select(self.elem("#queryClassSelector")).select_by_visible_text("Employee")
        self.elem("#submitClassSelect").click()
        self.elem('a[title="Show name in results"]').click()
        self.elem('a[title="Add a constraint to name"]').click()
        self.elem("#attribute8").clear()
        self.elem("#attribute8").send_keys(u"*ö*")
        self.elem("#attributeSubmit").click()
        self.run_and_expect(4)

    def test_run_query(self):
        query = """
        <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
          <constraint path="Bank.debtors.debt" op="&gt;" value="25000000" />
        </query>
        """
        self.findLink("Import query from XML").click()
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        self.run_and_expect(22)

    def run_and_expect(self, n):
        self.elem('#showResult').click()
        summary = self.elem(".im-table-summary")
        self.assertRowCountIs(n)

    def assertRowCountIs(self, n):
        self.assertEquals(n, len(self.elems('.im-table-container tbody tr')))

    def test_add_constraint_set_and(self):
        query = """
        <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
        </query>
        """
        expected_query = "\n".join([
            ' '.join([
                '<query',
                'name=""',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc"',
                'constraintLogic="A and B">'
            ]),
            '  <constraint path="Bank.debtors.debt" code="A" op="&gt;" value="35,000,000"/>',
            '  <constraint path="Bank.name" code="B" op="=" value="Gringotts"/>',
            '</query>'
        ])
        self.findLink("Import query from XML").click()
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        # Add constraint: Bank.debtors.debt > 35e6
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()
        # Add constraint: Bank.name = Gringotts
        self.elem('a[title="Add a constraint to name"]').click()
        Select(self.elem("#attribute7")).select_by_visible_text("Gringotts")
        self.elem('#attributeSubmit').click()
        # Check that the query is as expected.
        self.elem('a[title="Export this query as XML"]').click()
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()
        # Check that the results are as expected.
        self.elem('#showResult').click()
        self.assertRowCountIs(2)

    def test_add_constraint_set_or(self):
        query = """
        <query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt ASC" >
        </query>
        """
        expected_query = "\n".join([
            ' '.join([
                '<query',
                'name=""',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc"',
                'constraintLogic="A or B">'
            ]),
            '  <constraint path="Bank.debtors.debt" code="A" op="&gt;" value="35,000,000"/>',
            '  <constraint path="Bank.name" code="B" op="=" value="Gringotts"/>',
            '</query>'
        ])

        # Perform actions.
        self.findLink("Import query from XML").click()
        self.elem('#xml').send_keys(query)
        self.elem('#importQueriesForm input[type="submit"]').click()
        # Add constraint: Bank.debtors.debt > 35e6
        self.browser.find_element_by_id('img_Bank.debtors').click()
        self.elem('a[title="Add a constraint to debt"]').click()
        Select(self.elem("#attribute5")).select_by_visible_text(">")
        self.elem('#attribute8').send_keys('35,000,000')
        self.elem('#attributeSubmit').click()
        # Add constraint: Bank.name = Gringotts
        self.elem('a[title="Add a constraint to name"]').click()
        Select(self.elem("#attribute7")).select_by_visible_text("Gringotts")
        self.elem('#attributeSubmit').click()
        # Switch the constraint logic to A or B
        self.elem('#constraintLogic').click()
        logic = self.elem('#expr')
        logic.clear()
        logic.send_keys('A or B')
        self.browser.find_element_by_id('editconstraintlogic').click()

        # Check that the query is as expected.
        self.elem('a[title="Export this query as XML"]').click()
        self.assertEquals(expected_query.strip(), self.elem('body').text)
        self.browser.back()
        # Check that the results are as expected.
        self.elem('#showResult').click()
        self.assertRowCountIs(24)

    def load_queries_into_history(self):
        query_1 = ''.join([
            '<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '</query>'
            ])
        query_2 = ''.join([
            '<query model="testmodel" view="Bank.name Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '<constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>',
            '</query>'
            ])
        # Load queries into session history.
        for q in [query_1, query_2]:
            self.browser.get(self.base_url + '/customQuery.do')
            self.findLink("Import query from XML").click()
            self.elem('#xml').send_keys(q)
            self.elem('#importQueriesForm input[type="submit"]').click()
            self.elem('#showResult').click()
        self.browser.get(self.base_url + '/customQuery.do')

    def test_edit_template(self):
        self.browser.get(self.base_url + '/template.do?name=ManagerLookup&scope=all')
        self.elem('input.editQueryBuilder').click()
        self.assertIn('Query builder', self.browser.title)
        # Edit the constraint.
        self.elem('img[title="Edit this constraint"]').click()
        con_value = self.elem('#attribute8')
        con_value.clear()
        con_value.send_keys('Anne')
        self.elem('#attributeSubmit').click()
        # Check export.
        self.elem('a[title="Export this query as XML"]').click()
        expected_query = '\n'.join([
            '<query name="" model="testmodel" view="Manager.name Manager.title" longDescription="">',
            '  <constraint path="Manager" op="LOOKUP" value="Anne" extraValue=""/>',
            '</query>'])
        self.assertEquals(expected_query, self.elem('body').text)
        self.browser.back()
        # Check results.
        self.elem('#showResult').click()
        self.assertRowCountIs(1)

    def test_query_history(self):
        self.load_queries_into_history()
        self.assertIn('Custom query', self.browser.title)
        self.assertEquals(2, len(self.elems('#modifyQueryForm tbody tr')))
        self.assertEquals('query_2', self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(2)').text)
        root = self.elem('#modifyQueryForm tbody tr:nth-child(2) .historySummaryRoot').text
        self.assertEquals('Bank', root)
        showing = self.elems('#modifyQueryForm tbody tr:nth-child(2) .historySummaryShowing')
        self.assertEquals(2, len(showing))
        self.assertEquals(['Name', 'Debt'], [s.text for s in showing])

    def test_delete_query_from_history(self):
        self.load_queries_into_history()
        self.assertEquals(2, len(self.elems('#modifyQueryForm tbody tr')))
        self.elem('#selected_history_1').click()
        self.elem('#delete_button').click()
        Alert(self.browser).accept()
        self.assertEquals(1, len(self.elems('#modifyQueryForm tbody tr')))

    def test_run_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(1)').click()
        self.assertRowCountIs(16)

    def test_edit_query_in_query_history(self):
        self.load_queries_into_history()

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(2)').click()
        self.assertIn('Query builder', self.browser.title)
        self.assertEquals('Bank', self.elem('.typeSelected').text)
        # Edit a constraint.
        self.elem('img[title="Edit this constraint"]').click()
        con_value = self.elem('#attribute8')
        con_value.clear()
        con_value.send_keys('40,000,000')
        self.elem('#attributeSubmit').click()
        # Check results.
        self.elem('#showResult').click()
        self.assertRowCountIs(15)

    def test_export_query_in_query_history(self):
        self.load_queries_into_history()
        expected_query = '\n'.join([
            ' '.join([
                '<query',
                'name="query_2"',
                'model="testmodel"',
                'view="Bank.name Bank.debtors.debt"',
                'longDescription=""',
                'sortOrder="Bank.debtors.debt asc">'
                ]),
            '  <constraint path="Bank.debtors.debt" op="&gt;" value="35,000,000"/>',
            '</query>'])

        self.elem('#modifyQueryForm tbody tr:nth-child(2) td:nth-child(7) span.fakelink:nth-child(3)').click()
        self.assertEquals(expected_query, self.elem('body').text)

    def test_import_query(self):
        link = self.findLink("Import query from XML")
        self.assertIsNotNone(link)
        link.click()
        self.assertIn('Import Query', self.browser.title)
        input_box = self.elem('#xml')
        self.assertIsNotNone(input_box)
        query = ''.join([
            '<query model="testmodel" view="Bank.debtors.debt" sortOrder="Bank.debtors.debt asc">',
            '<constraint path="Bank.debtors.debt" op="&gt;" value="1000"/>',
            '</query>'
            ])

        input_box.send_keys(query)
        self.assertEquals('true', self.elem('#file').get_attribute('disabled'))
        self.elem('#importQueriesForm input[type="submit"]').click()
        wait = WebDriverWait(self.browser, 10)
        wait.until(EC.title_contains('Query builder'))

        self.assertEquals('Bank', self.elem('.typeSelected').text)
        constraints = self.elems('span.constraint')
        self.assertEquals(1, len(constraints))
        self.assertEquals('> 1000', constraints[0].text)
        self.assertEquals(1, len(self.browser.find_elements_by_class_name('viewpath')))

    def test_login_to_view_saved(self):
        link = self.findLink("Login to view saved queries")
        self.assertIsNotNone(link)