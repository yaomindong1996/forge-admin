package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.ContactMemberQuery;
import com.mdframe.forge.plugin.system.vo.ContactMemberVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SysContactsServiceImplTest {

    @Test
    void pageSizeIsCappedAndDefaultsAreApplied() {
        ContactMemberQuery query = new ContactMemberQuery();
        query.setPageNum(0);
        query.setPageSize(500);

        ContactMemberQuery normalized = SysContactsServiceImpl.normalizeQuery(query);

        assertThat(normalized.getPageNum()).isEqualTo(1);
        assertThat(normalized.getPageSize()).isEqualTo(SysContactsServiceImpl.MAX_PAGE_SIZE);

        ContactMemberQuery empty = SysContactsServiceImpl.normalizeQuery(null);
        assertThat(empty.getPageNum()).isEqualTo(1);
        assertThat(empty.getPageSize()).isEqualTo(SysContactsServiceImpl.DEFAULT_PAGE_SIZE);
        assertThat(empty.getKeyword()).isNull();
    }

    @Test
    void keywordIsTrimmedAndLengthLimited() {
        ContactMemberQuery query = new ContactMemberQuery();
        query.setKeyword("  张三  ");
        assertThat(SysContactsServiceImpl.normalizeQuery(query).getKeyword()).isEqualTo("张三");

        query.setKeyword("   ");
        assertThat(SysContactsServiceImpl.normalizeQuery(query).getKeyword()).isNull();

        query.setKeyword("a".repeat(80));
        assertThat(SysContactsServiceImpl.normalizeQuery(query).getKeyword())
                .hasSize(SysContactsServiceImpl.MAX_KEYWORD_LENGTH);
    }

    @Test
    void memberVoOnlyExposesDisplayFields() {
        String[] fields = Arrays.stream(ContactMemberVO.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .sorted()
                .toArray(String[]::new);

        assertThat(fields).containsExactly(
                "avatar", "email", "orgNames", "phone", "postNames", "realName", "userId");
    }
}
