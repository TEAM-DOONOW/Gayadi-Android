import http from "node:http";

const port = Number(process.argv[2] ?? 14100);

const tourItem = {
  contentid: "900001",
  contenttypeid: "14",
  title: "연동 테스트 박물관",
  addr1: "서울 중구 테스트로 1",
  addr2: "",
  zipcode: "00000",
  tel: "",
  firstimage: "https://example.invalid/place.jpg",
  firstimage2: "",
  mapx: "126.9780",
  mapy: "37.5665",
  mlevel: "6",
  createdtime: "20260901000000",
  modifiedtime: "20260901000000",
  cpyrhtDivCd: "Type1",
  lDongRegnCd: "11",
  lDongSignguCd: "110",
  lclsSystm1: "NA",
  lclsSystm2: "NA04",
  lclsSystm3: "NA040100",
  dist: "120",
  eventstartdate: "20260901",
  eventenddate: "20261231",
  progresstype: "진행중",
  festivaltype: "문화관광축제",
};

function envelope(items) {
  return {
    response: {
      header: { resultCode: "0000", resultMsg: "OK" },
      body: { items: { item: items }, totalCount: items.length, numOfRows: 100, pageNo: 1 },
    },
  };
}

function weatherEnvelope(items) {
  return {
    response: {
      header: { resultCode: "00", resultMsg: "NORMAL_SERVICE" },
      body: { items: { item: items }, totalCount: items.length, numOfRows: 1000, pageNo: 1 },
    },
  };
}

function send(response, body, status = 200) {
  const encoded = JSON.stringify(body);
  response.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(encoded),
  });
  response.end(encoded);
}

http.createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);

  if (url.pathname.includes("chat/completions")) {
    send(response, {
      id: "local-test",
      object: "chat.completion",
      created: 1,
      model: "local-test",
      choices: [{ index: 0, finish_reason: "stop", message: { role: "assistant", content: "{}" } }],
      usage: { prompt_tokens: 1, completion_tokens: 1, total_tokens: 2 },
    });
    return;
  }

  if (url.pathname.includes("detailCommon2") || url.pathname.includes("detailIntro2")) {
    send(response, envelope([{ overview: "에뮬레이터 연동 테스트 장소", usetimeculture: "09:00~18:00", parkingculture: "가능" }]));
    return;
  }
  if (url.pathname.includes("ldongCode2")) {
    send(response, envelope([{ code: "110", name: "서울" }]));
    return;
  }
  if (url.pathname.startsWith("/tour/")) {
    send(response, envelope([tourItem]));
    return;
  }

  if (url.pathname.endsWith("getFcstVersion")) {
    send(response, weatherEnvelope([{ filetype: url.searchParams.get("ftype") ?? "SHRT", version: "1.0" }]));
    return;
  }
  if (url.pathname.endsWith("getUltraSrtNcst")) {
    const baseDate = url.searchParams.get("base_date") ?? "20260910";
    const baseTime = url.searchParams.get("base_time") ?? "1200";
    const values = { T1H: "23.0", RN1: "0", UUU: "1.0", VVV: "0.5", REH: "50", PTY: "0", VEC: "180", WSD: "1.2" };
    send(response, weatherEnvelope(Object.entries(values).map(([category, obsrValue]) => ({
      baseDate, baseTime, nx: 60, ny: 127, category, obsrValue,
    }))));
    return;
  }
  if (url.pathname.endsWith("getUltraSrtFcst") || url.pathname.endsWith("getVilageFcst")) {
    const baseDate = url.searchParams.get("base_date") ?? "20260910";
    const baseTime = url.searchParams.get("base_time") ?? "1200";
    const categories = url.pathname.endsWith("getUltraSrtFcst")
      ? { T1H: "24", SKY: "1", PTY: "0", RN1: "0", POP: "10" }
      : { TMP: "24", SKY: "1", PTY: "0", PCP: "강수없음", POP: "10" };
    send(response, weatherEnvelope(Object.entries(categories).map(([category, fcstValue]) => ({
      baseDate, baseTime, fcstDate: baseDate, fcstTime: "1500", nx: 60, ny: 127, category, fcstValue,
    }))));
    return;
  }

  send(response, { error: "unsupported local test path" }, 404);
}).listen(port, "127.0.0.1", () => {
  process.stdout.write(`mock external API listening on ${port}\n`);
});
