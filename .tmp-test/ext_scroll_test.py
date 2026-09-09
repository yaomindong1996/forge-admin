import json

from playwright.sync_api import sync_playwright

BASE = 'http://localhost:3001'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1600, 'height': 900})

    page.goto(f'{BASE}/login')
    page.wait_for_load_state('networkidle')
    page.fill('input[placeholder*="用户名"], input[type="text"]', 'admin')
    page.fill('input[placeholder*="密码"], input[type="password"]', '123456')
    page.keyboard.press('Enter')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)

    page.goto(f'{BASE}/external/system')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2500)
    page.screenshot(path='/tmp/ext-list.png')

    links = page.get_by_text('接口', exact=True)
    print('api link count:', links.count())
    if links.count() > 0:
        links.first.click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    page.screenshot(path='/tmp/ext-api-list.png')

    edit_btns = page.get_by_text('编辑', exact=True)
    print('edit btn count:', edit_btns.count())
    if edit_btns.count() > 0:
        edit_btns.first.click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    page.screenshot(path='/tmp/ext-api-edit-top.png')

    body = page.locator('.inline-form-panel-body')
    print('panel body count:', body.count())
    if body.count() > 0:
        state = page.evaluate('''() => {
            const el = document.querySelector('.inline-form-panel-body')
            if (!el) return null
            el.scrollTop = 800
            return {scrollHeight: el.scrollHeight, clientHeight: el.clientHeight, scrollTop: el.scrollTop, canScroll: el.scrollHeight > el.clientHeight}
        }''')
        print('scroll state:', json.dumps(state))
        page.wait_for_timeout(500)
        state2 = page.evaluate('''() => {
            const el = document.querySelector('.inline-form-panel-body')
            return el ? {scrollTop: el.scrollTop, scrollHeight: el.scrollHeight, clientHeight: el.clientHeight} : null
        }''')
        print('after 500ms:', json.dumps(state2))
        page.screenshot(path='/tmp/ext-api-edit-scrolled.png')

    chain = page.evaluate('''() => {
        const pick = (sel) => {
            const el = document.querySelector(sel)
            if (!el) return null
            const cs = getComputedStyle(el)
            return {sel, h: el.offsetHeight, scrollH: el.scrollHeight, ov: cs.overflow + '/' + cs.overflowY, cls: String(el.className).slice(0, 70)}
        }
        return [
            pick('.content-area'), pick('.external-manage-page'), pick('.panel-card'),
            pick('.n-card__content'), pick('.ai-crud-page'), pick('.ai-crud-main'),
            pick('.ai-crud-inline-workspace'), pick('.ai-crud-inline-form-panel'),
            pick('.inline-form-panel-body'),
        ]
    }''')
    print('height chain:')
    for item in chain:
        print(' ', json.dumps(item, ensure_ascii=False))

    browser.close()
