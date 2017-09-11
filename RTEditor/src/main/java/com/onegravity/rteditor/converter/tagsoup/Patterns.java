// This file is part of TagSoup and is Copyright 2002-2008 by John Cowan.
//
// TagSoup is licensed under the Apache License,
// Version 2.0.  You may obtain a copy of this license at
// http://www.apache.org/licenses/LICENSE-2.0 .  You may also have
// additional legal rights not granted by this license.
//
// TagSoup is distributed in the hope that it will be useful, but
// unless required by applicable law or agreed to in writing, TagSoup
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, either express or implied; not even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

package com.onegravity.rteditor.converter.tagsoup;

import java.util.regex.Pattern;

/**
 * We use this class instead of android.util.Patterns since we want to use the latest version of the top level domain list from
 * http://data.iana.org/TLD/tlds-alpha-by-domain.txt (Version 2014091501, Last Updated Tue Sep 16 07:07:01 2014 UTC)
 */
public class Patterns {

    public static final String GOOD_IRI_CHAR =
            "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:"
                    + "(?:axa|aero|army|arpa|asia|actor|archi|audio|autos|active|agency|academy|auction|airforce|attorney|associates|accountants|a[cdefgilmnoqrstuwxz])"
                    + "|(?:bar|bid|bio|biz|bmw|boo|bzh|beer|best|bike|blue|buzz|black|build|bayern|berlin|bargains|boutique|brussels|builders|business|bnpparibas|blackfriday|b[abdefghijmnorstvwyz])"
                    + "|(?:cab|cal|cat|ceo|com|camp|care|cash|cern|city|club|cool|coop|cards|cheap|citic|click|codes|cymru|camera|career|center|chrome|church|claims|clinic|coffee|condos|credit|capital|caravan|careers|channel|collee|cologne|company|cooking|country|cruises|capetown|catering|cleaning|clothing|computer|christmas|community|consulting|creditcard|cuisinella|contractors|construction|cancerresearch|c[acdfghiklmnoruvwxyz])"
                    + "|(?:dad|day|dnp|desi|diet|dance|deals|dating|degree|dental|direct|durban|dentist|digital|domains|democrat|diamonds|discount|directory|d[ejkmoz])"
                    + "|(?:eat|edu|esq|eus|email|estate|events|expert|exposed|engineer|exchange|education|equipment|engineering|enterprises|e[cegrstu])"
                    + "|(?:fly|foo|frl|fail|farm|fish|fund|futbol|finance|fishing|fitness|flights|florist|frogans|feedback|financial|furniture|foundation|f[ijkmor])"
                    + "|(?:gal|gle|gmo|gmx|gop|gov|gbiz|gent|gift|guru|gifts|gives|glass|globo|gmail|green|gripe|guide|global|google|gratis|gallery|guitars|graphics|g[abdefghilmnpqrstuwy])"
                    + "|(?:hiv|how|haus|help|here|host|homes|horse|house|hiphop|hamburg|holiday|hosting|holdings|healthcare|h[kmnrtu])"
                    + "|(?:ing|ink|int|immo|info|insure|institute|immobilien|industries|investments|international|i[delmnoqrst])"
                    + "|(?:jobs|jetzt|joburg|juegos|j[emop])"
                    + "|(?:kim|krd|kiwi|kred|koeln|kaufen|kitchen|k[eghimnprwyz])"
                    + "|(?:land|lgbt|life|limo|link|ltda|luxe|lease|loans|lotto|lawyer|london|luxury|lacaixa|limited|lighting|l[abcikrstuvy])"
                    + "|(?:mil|moe|mov|meet|meme|menu|mini|mobi|moda|mango|media|miami|maison|market|monash|moscow|museum|mortgage|marketing|melbourne|management|motorcycles|m[acdeghklmnopqrstuvwxyz])"
                    + "|(?:net|new|ngo|nhk|nra|nrw|nyc|name|navy|nexus|ninja|nagoya|network|neustar|n[acefgilopruz])"
                    + "|(?:ong|onl|ooo|org|ovh|otsuka|okinawa|organic|om)"
                    + "|(?:pro|pub|pics|pink|post|prod|prof|paris|parts|photo|pizza|place|praxi|press|photos|physio|partners|pharmacy|pictures|plumbing|property|properties|photography|productions|p[aefghklmnrstwy])"
                    + "|(?:qpon|quebec|qa)"
                    + "|(?:red|ren|rio|rest|rich|rsvp|ruhr|rehab|reise|rocks|rodeo|reisen|repair|report|ryukyu|realtor|recipes|rentals|reviews|republican|restaurant|r[eosuw])"
                    + "|(?:sca|scb|soy|sarl|scot|sexy|sohu|surf|shoes|solar|space|schule|social|supply|suzuki|schmidt|shiksha|singles|spiegel|support|surgery|systems|saarland|services|software|supplies|solutions|s[abcdeghijklmnortuvxyz])"
                    + "|(?:tax|tel|top|tips|town|toys|tatar|tirol|today|tokyo|tools|trade|tattoo|tienda|travel|training|technology|t[cdfghjklmnoprtvwz])"
                    + "|(?:uno|uol|university|u[agksyz])"
                    + "|(?:vet|vote|voto|vegas|vodka|viajes|villas|vision|voting|voyage|ventures|vacations|vlaanderen|versicherung|v[aceginu])"
                    + "|(?:wed|wme|wtc|wtf|wang|wien|wiki|wales|watch|works|webcam|website|whoswho|williamhill|w[fs])"
                    + "|(?:xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah)"
                    + "|(?:xxx|xyz)"
                    + "|(?:yachts|yandex|youtube|yokohama|y[et])"
                    + "|(?:zip|zone|z[amw])))";

    public static final Pattern WEB_URL = Pattern.compile(
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)"); // and finally, a word boundary or end of
    // input.  This is to stop foo.sure from
    // matching as foo.su

}