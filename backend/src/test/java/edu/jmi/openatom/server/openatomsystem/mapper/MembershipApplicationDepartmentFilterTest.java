package edu.jmi.openatom.server.openatomsystem.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MembershipApplicationDepartmentFilterTest {
  private SqlSession session;
  private MembershipApplicationMapper mapper;

  @BeforeEach
  void setUp() throws Exception {
    var dataSource = new UnpooledDataSource(
        "org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
    var configuration = new MybatisConfiguration();
    configuration.setEnvironment(new Environment(
        "test", new JdbcTransactionFactory(), dataSource));
    var pagination = new MybatisPlusInterceptor();
    pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
    configuration.addInterceptor(pagination);
    configuration.addMapper(MembershipApplicationMapper.class);
    session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession();
    mapper = session.getMapper(MembershipApplicationMapper.class);
    try (var statement = session.getConnection().createStatement()) {
      statement.execute("""
          CREATE TABLE membership_application (
            id INT PRIMARY KEY, campaign_id INT, club_id INT, user_id INT,
            first_choice_department_id INT, second_choice_department_id INT,
            status VARCHAR(50), profile VARCHAR(255),
            created_at TIMESTAMP, updated_at TIMESTAMP
          )
          """);
      statement.execute("""
          INSERT INTO membership_application
            (id, campaign_id, club_id, first_choice_department_id,
             second_choice_department_id, status, profile)
          VALUES
            (1, 9, 1, 10, 20, 'pre_screen_passed', '{"name":"Alice"}'),
            (2, 9, 1, 20, 10, 'pre_screen_passed', '{"name":"Bob"}'),
            (3, 9, 1, 10, 20, 'interviewed', '{"name":"Carol"}'),
            (4, 8, 1, 10, 20, 'pre_screen_passed', '{"name":"Alice"}'),
            (5, 9, 2, 10, 20, 'pre_screen_passed', '{"name":"Alice"}'),
            (6, 9, 1, NULL, 10, 'pre_screen_passed', '{"name":"Dan"}')
          """);
    }
  }

  @AfterEach
  void tearDown() {
    if (session != null) session.close();
  }

  @Test
  void departmentFilterIncludesOnlyFirstChoiceAndPaginatesCorrectly() {
    var first = mapper.selectPageByConditions(
        new Page<>(1, 1), 9, 1, null, 10, null, List.of());
    assertEquals(2, first.getTotal());
    assertEquals(List.of(3), ids(first.getRecords()));

    var second = mapper.selectPageByConditions(
        new Page<>(2, 1), 9, 1, null, 10, null, List.of());
    assertEquals(List.of(1), ids(second.getRecords()));
  }

  @Test
  void exportAndCandidateSelectionUseTheSameFirstChoiceFilter() {
    var exported = mapper.selectAllByConditions(9, 1, null, 10, null, List.of());
    assertEquals(List.of(3, 1), ids(exported));

    var schedulable = mapper.selectPageByConditions(
        new Page<>(1, 100), 9, 1, "pre_screen_passed", 10, null, List.of());
    assertEquals(List.of(1), ids(schedulable.getRecords()));
  }

  @Test
  void departmentFilterCombinesWithCampaignClubStatusAndKeyword() {
    var page = mapper.selectPageByConditions(
        new Page<>(1, 10), 9, 1, "pre_screen_passed", 10, "Alice", List.of());
    var exported = mapper.selectAllByConditions(
        9, 1, "pre_screen_passed", 10, "Alice", List.of());
    assertEquals(List.of(1), ids(page.getRecords()));
    assertEquals(ids(page.getRecords()), ids(exported));
  }

  @Test
  void clearingDepartmentRestoresAllApplicationsInTheCampaign() {
    var page = mapper.selectPageByConditions(
        new Page<>(1, 10), 9, 1, null, null, null, List.of());
    var exported = mapper.selectAllByConditions(9, 1, null, null, null, List.of());
    assertEquals(List.of(6, 3, 2, 1), ids(page.getRecords()));
    assertEquals(ids(page.getRecords()), ids(exported));
  }

  private List<Integer> ids(List<MembershipApplication> applications) {
    return applications.stream().map(MembershipApplication::getId).toList();
  }
}
