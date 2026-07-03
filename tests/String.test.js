// Rhino shell test for modules/core/String.js
// Run via: ./gradlew testJs
//
// Mocks the minimal Helma globals needed to load the module without a running server.
var app = { addRepository: function() {} };

load('modules/core/String.js');

var passed = 0, failed = 0;

function check(label, input, expect, pattern) {
  var result = pattern.test(input);
  if (result === expect) {
    print('✓ ' + label);
    passed++;
  } else {
    print('✗ FAIL ' + label + ' → expected ' + expect + ', got ' + result);
    failed++;
  }
}

function email(label, input, expect) { check(label, input, expect, String.EMAILPATTERN); }
function url(label, input, expect)   { check(label, input, expect, String.URLPATTERN); }

// ── Email: valid ──────────────────────────────────────────────────────────────
print('=== Email — valid ===');
email('user@example.com',                        'user@example.com',                     true);
email('user!name@example.com (! in local)',       'user!name@example.com',                true);
email('用户@例子.广告 (Unicode IDN)',        '用户@例子.广告',                       true);
email('user@[1.2.3.4] (IPv4 literal)',           'user@[1.2.3.4]',                       true);
email('user@10.0.0.1 (bare IPv4)',               'user@10.0.0.1',                        true);
email('user@sub-domain.example.com (hyphen)',    'user@sub-domain.example.com',          true);
email('a@b.io (short)',                          'a@b.io',                               true);
email('.user@example.com (leading dot in local)', '.user@example.com',                   true);
email('user@123domain.com (digit-start label)',  'user@123domain.com',                   true);
email('user+tag@sub.domain.co.uk',              'user+tag@sub.domain.co.uk',             true);
email("'sql'@example.com (apostrophe in local)", "'sql'@example.com",                    true);
email('user@xn--nxasmq6b.com (punycode)',        'user@xn--nxasmq6b.com',               true);
email('user@exаmple.com (Cyrillic а, IDN)',     'user@exаmple.com',               true);

// ── Email: RFC 5321 atext chars still pass in local part ─────────────────────
print('\n=== Email — RFC atext chars still pass ===');
email('# in local', 'user#name@example.com', true);
email('$ in local', 'user$name@example.com', true);
email('% in local', 'user%name@example.com', true);
email('& in local', 'user&name@example.com', true);
email('* in local', 'user*name@example.com', true);
email('/ in local', 'user/name@example.com', true);
email('= in local', 'user=name@example.com', true);
email('? in local', 'user?name@example.com', true);
email('^ in local', 'user^name@example.com', true);
email('_ in local', 'user_name@example.com', true);
email('~ in local', 'user~name@example.com', true);

// ── Email: invalid (structural) ───────────────────────────────────────────────
print('\n=== Email — invalid: structural ===');
email('notanemail',                              'notanemail',                           false);
email('user@ (empty domain)',                   'user@',                                false);
email('@domain.com (empty local)',              '@domain.com',                           false);
email('user @x.com (space)',                    'user @x.com',                          false);
email('user@domain (no TLD)',                   'user@domain',                          false);
email('user@@example.com (double @)',           'user@@example.com',                    false);
email('user@[IPv6:2001:db8::1] (IPv6 literal)', 'user@[IPv6:2001:db8::1]',             false);
email('user@invalid! (! in domain label)',      'user@invalid!',                        false);
email('user@example.com. (trailing dot)',        'user@example.com.',                    false);
email('user@example..com (double dot)',          'user@example..com',                    false);
email('"quoted local"@example.com (space)',      '"quoted local"@example.com',           false);
email('-- comment @example.com (space)',         '-- comment @example.com',              false);

// ── Email: non-atext chars blocked from local part ────────────────────────────
print('\n=== Email — non-atext chars blocked from local part ===');
email('< in local',  '<script>@example.com',    false);
email('> in local',  'user>name@example.com',   false);
email('[ in local',  'user[name@example.com',   false);
email('] in local',  'user]name@example.com',   false);
email('( in local',  'user(name@example.com',   false);
email(') in local',  'user)name@example.com',   false);
email(', in local',  'user,name@example.com',   false);
email('; in local',  'user;name@example.com',   false);
email(': in local',  'user:name@example.com',   false);

// ── Email: null / control bytes ───────────────────────────────────────────────
print('\n=== Email — null/control bytes ===');
email('null byte in local',   'user\x00@example.com',   false);
email('null byte in domain',  'user@exam\x00ple.com',   false);
email('trailing null byte',   'user@example.com\x00',   false);
email('trailing newline',     'user@example.com\n',     false);
email('trailing space',       'user@example.com ',      false);
email('CR in domain',         'user@exam\rple.com',     false);
email('LF in domain',         'user@exam\nple.com',     false);
email('tab in domain',        'user@exam\tple.com',     false);
email('NBSP in domain',       'user@exam ple.com', false);

// ── Email: invisible / bidi Unicode ──────────────────────────────────────────
print('\n=== Email — invisible/bidi Unicode ===');
email('U+200B ZW-space in domain',        'user@exam​ple.com', false);
email('U+200C ZW-non-joiner in domain',   'user@exam‌ple.com', false);
email('U+200D ZW-joiner in domain',       'user@exam‍ple.com', false);
email('U+200E LTR-mark in domain',        'user@exam‎ple.com', false);
email('U+200F RTL-mark in domain',        'user@exam‏ple.com', false);
email('U+2060 word-joiner in domain',     'user@exam⁠ple.com', false);
email('U+202A LTR-embedding in domain',   'user@exam‪ple.com', false);
email('U+202B RTL-embedding in domain',   'user@exam‫ple.com', false);
email('U+202C pop-dir in domain',         'user@exam‬ple.com', false);
email('U+202D LTR-override in domain',    'user@exam‭ple.com', false);
email('U+202E RTL-override in domain',    'user@exam‮ple.com', false);
email('U+2066 bidi-isolate in domain',    'user@exam⁦ple.com', false);
email('U+00AD soft-hyphen in domain',     'user@exam­ple.com', false);
email('U+200B ZW-space in local part',    'user​name@example.com', false);
email('U+202E RTL-override in local part','user‮name@example.com', false);

// ── Email: ReDoS ─────────────────────────────────────────────────────────────
print('\n=== Email — ReDoS attack ===');
var t = Date.now();
email('16-label attack (must fail fast)', 'user@a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.invalid!', false);
var ms = Date.now() - t;
if (ms > 500) {
  print('✗ FAIL ReDoS guard: pattern took ' + ms + 'ms (limit 500ms)');
  failed++;
} else {
  print('✓ ReDoS timing: ' + ms + 'ms');
  passed++;
}

// ── URL: valid ───────────────────────────────────────────────────────────────
print('\n=== URL — valid ===');
url('http://example.com',                        'http://example.com',                   true);
url('https://例子.广告/path (IDN)',          'https://例子.广告/path',               true);
url('http://10.0.0.1:8080/',                     'http://10.0.0.1:8080/',                true);
url('http://localhost (no TLD)',                  'http://localhost',                     true);
url('http://user:pass@example.com (credentials)', 'http://user:pass@example.com',        true);
url('ftp://files.example.org',                   'ftp://files.example.org',              true);
url('http://sub-domain.example.com (hyphen)',    'http://sub-domain.example.com',        true);
url('http://example.com/path?q=hi!&r=x#top (! in query)', 'http://example.com/path?q=hi!&r=x#top', true);
url('http://example.com/path!here (! in path)',  'http://example.com/path!here',         true);
url('http://xn--nxasmq6b.com (punycode)',        'http://xn--nxasmq6b.com',             true);
url('http://example.com/%0d%0a (CRLF pct-encoded)', 'http://example.com/%0d%0a',        true);
url("http://example.com/path?a='b'&c=d",        "http://example.com/path?a='b'&c=d",   true);
url('http://0x7f000001 (hex IP)',                'http://0x7f000001',                    true);
url('http://2130706433 (decimal IP)',            'http://2130706433',                    true);
url('http://example.com:65536 (high port)',      'http://example.com:65536',             true);

// ── URL: invalid ──────────────────────────────────────────────────────────────
print('\n=== URL — invalid ===');
url('javascript:alert(1)',                       'javascript:alert(1)',                   false);
url('JAVASCRIPT:alert(1) (uppercase)',           'JAVASCRIPT:alert(1)',                   false);
url('Javascript:alert(1) (mixed case)',          'Javascript:alert(1)',                   false);
url('data:text/html,<script>',                  'data:text/html,<script>',               false);
url('vbscript:msgbox(1)',                        'vbscript:msgbox(1)',                    false);
url('file:///etc/passwd',                        'file:///etc/passwd',                    false);
url('http:// (no host)',                         'http://',                               false);
url('http://exa!mple.com (! in host)',           'http://exa!mple.com',                  false);
url('//example.com (no scheme)',                 '//example.com',                         false);
url('http://example.com. (trailing dot)',        'http://example.com.',                   false);
url('notaurl',                                   'notaurl',                               false);
url('http://[::1] (IPv6 literal)',               'http://[::1]',                          false);
url('null byte in host',                         'http://exam\x00ple.com',               false);
url('null byte in path',                         'http://example.com/\x00path',          false);
url('trailing null byte',                        'http://example.com\x00',               false);
url('trailing newline',                          'http://example.com\n',                  false);
url('trailing space',                            'http://example.com ',                   false);
url('LF in host',                                'http://exam\nple.com',                  false);
url('CR in host',                                'http://exam\rple.com',                  false);
url('backslash-at host confusion',               'http://evil.com\\@good.com',           false);
url('space in path',                             'http://example.com/path here',         false);
url('backslash in host',                         'http://exa\\mple.com',                 false);

// ── URL: invisible / bidi Unicode ─────────────────────────────────────────────
print('\n=== URL — invisible/bidi Unicode ===');
url('U+200B ZW-space in host',    'http://exam​ple.com',          false);
url('U+202E RTL-override in host','http://exam‮ple.com',          false);
url('U+00AD soft-hyphen in host', 'http://exam­ple.com',          false);
url('U+200E LTR-mark in host',    'http://exam‎ple.com',          false);
url('U+2060 word-joiner in host', 'http://exam⁠ple.com',          false);
url('U+200B ZW-space in path',    'http://example.com/​path',     false);
url('U+202E RTL-override in path','http://example.com/‮path',     false);

// ── URL: ReDoS ────────────────────────────────────────────────────────────────
print('\n=== URL — ReDoS attack ===');
var t2 = Date.now();
url('16-label attack (must fail fast)', 'http://a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.invalid!', false);
var ms2 = Date.now() - t2;
if (ms2 > 500) {
  print('✗ FAIL ReDoS guard: pattern took ' + ms2 + 'ms (limit 500ms)');
  failed++;
} else {
  print('✓ ReDoS timing: ' + ms2 + 'ms');
  passed++;
}

// ── Summary ───────────────────────────────────────────────────────────────────
print('\nPassed: ' + passed + ', Failed: ' + failed);
if (failed > 0) quit(1);
