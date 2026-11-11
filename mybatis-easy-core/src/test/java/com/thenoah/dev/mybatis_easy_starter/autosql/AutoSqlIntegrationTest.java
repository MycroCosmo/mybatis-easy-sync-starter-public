package com.thenoah.dev.mybatis_easy_starter.autosql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:autosql;DB_CLOSE_DELAY=-1",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis-easy.autosql.enabled=true",
        "mybatis-easy.autosql.generated-key.key-column=member_id",
        "mybatis-easy.pagination.enabled=true",
        "mybatis-easy.pagination.max-page-size=2"
})
@Transactional
class AutoSqlIntegrationTest {

    @Autowired
    MemberMapper mapper;

    @ParameterizedTest
    @CsvSource({"-5,0,c", "0,-3,c", "0,999,c;b", "1,1,b", "3,1,"})
    void paginationExecutesWithBoundedArguments(long offset, int limit, String names) {
        for (String name : new String[]{"a", "b", "c"}) {
            Member member = new Member();
            member.setName(name);
            mapper.insert(member);
        }

        assertThat(mapper.findPage(offset, limit)).extracting(Member::getName)
                .containsExactly(names == null ? new String[0] : names.split(";"));
    }

    @Test
    void crudWorksWhenPkFieldIsNotNamedId() {
        Member m = new Member();
        m.setName("alice");
        assertThat(mapper.insert(m)).isEqualTo(1);
        assertThat(m.getMemberId()).isNotNull();

        Long id = m.getMemberId();
        assertThat(mapper.findById(id)).get().extracting(Member::getName).isEqualTo("alice");

        Member patch = new Member();
        patch.setMemberId(id);
        patch.setName("bob");
        assertThat(mapper.update(patch)).isEqualTo(1);
        assertThat(mapper.findById(id)).get().extracting(Member::getName).isEqualTo("bob");

        assertThat(mapper.findAll()).hasSize(1);

        assertThat(mapper.deleteById(id)).isEqualTo(1);
        assertThat(mapper.findById(id)).isEmpty();
    }
}
