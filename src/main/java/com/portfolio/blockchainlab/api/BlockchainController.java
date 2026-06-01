package com.portfolio.blockchainlab.api;

import com.portfolio.blockchainlab.core.Block;
import com.portfolio.blockchainlab.core.Blockchain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chain")
public class BlockchainController {
    private final Blockchain blockchain;

    public BlockchainController(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @GetMapping
    public ChainResponse getChain() {
        // 현재 체인 상태를 그대로 노출해 hash 연결과 validation 결과를 관찰한다.
        return new ChainResponse(blockchain.height(), blockchain.isValid(), blockchain.blocks());
    }

    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public Block addBlock(@Valid @RequestBody AddBlockRequest request) {
        // API 요청 하나가 새 블록 채굴로 이어지는 가장 단순한 흐름이다.
        return blockchain.addBlock(request.data());
    }

    public record AddBlockRequest(@NotBlank String data) {
    }

    public record ChainResponse(long height, boolean valid, List<Block> blocks) {
    }
}
