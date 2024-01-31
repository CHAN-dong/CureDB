//SPDX-License-Identifier: MIT
pragma solidity ^0.8.16;

contract Sqbf_sig{

    event LogUint(uint256);
    event LogBytes1(bytes1);
    event LogBool(bool);


    struct Tag {
        uint tokenTag;
        uint voPathTag;
        uint resTag;
        uint n;
    }

    struct Sig {
        uint d_x;
        uint d_y;
        uint s;
    }

    function verifyRes(bytes[][] memory delta_w_list, bytes calldata voPath, bytes memory token, bytes[] memory res, Sig memory sig) public {
        Tag memory tag;
        tag.n = delta_w_list.length;
        uint[] memory version = new uint[](tag.n);
        for (uint i = 0; i < tag.n; ++i) {
            version[i] = delta_w_list[i].length;
        }
        emit LogBool(verify(verify_for_tree(version, token, voPath, delta_w_list, tag, "", res), sig.d_x, sig.d_y, sig.s));
    }

    function verify_for_tree(uint[] memory version, bytes memory token, bytes calldata voPath, bytes[][] memory delta_w_list, Tag memory tag, bytes memory path, bytes[] memory res) internal returns(uint h_m){
        h_m = 0;
        if (token.length != 0) {
            tag.tokenTag++;//token:<
            if (token[tag.tokenTag] == '>') {
                tag.tokenTag++;//>
                token = "";
            }
        }
        tag.voPathTag++;//<
        if (voPath[tag.voPathTag] == '>') {
            tag.voPathTag++;
            hashToUint(abi.encodePacked(path, res[tag.resTag++]));
        } else {
            bytes[] memory chdVersionStr = new bytes[](tag.n);
            uint[][] memory chdVersion = new uint[][](4);
            
            for (uint i = 0; i < 4; ++i) {chdVersion[i] = new uint[](tag.n);}
            for (uint i = 0; i < tag.n; ++i) {
                tag.voPathTag++;//(
                uint start = tag.voPathTag;
                for (uint c = 0; c < 4; ++c) {
                    chdVersion[c][i] = uint8(voPath[tag.voPathTag]) - 0x30;
                    tag.voPathTag += 2;//chdVersion
                }
                chdVersionStr[i] = voPath[start : tag.voPathTag - 1];
            }

            for (uint i = 0; i < tag.n; ++i) {
                h_m = addmod(h_m, uint256(keccak256(abi.encodePacked(delta_w_list[i][version[i] - 1], path, chdVersionStr[i]))), q);
            }

            for (uint c = 0; c < 4; ++c) {
                if (token.length != 0 && (token[tag.tokenTag] == ';' || token[tag.tokenTag] == '>')) {//如果这个孩子没有被查询则跳过
                    tag.tokenTag += 1;//
                    tag.voPathTag++;
                    continue;
                }
                bool isContinue = true;

                for (uint j = 0; j < tag.n; ++j) {
                    if (chdVersion[c][j] == 0) {
                        isContinue = false;
                        tag.voPathTag++;
                        break;
                    }
                }
                if (isContinue && voPath[tag.voPathTag] == '<') {//
                    h_m = addmod(h_m, verify_for_tree(chdVersion[c], token, voPath, delta_w_list, tag, abi.encodePacked(path, c), res), q);
                    if(token.length != 0) tag.tokenTag += 1;
                    tag.voPathTag += 1;
                }
            }
        }
    }

    function hashToUint(bytes memory input) pure public returns(uint256) {
        return uint256(keccak256(input));
    }

    function stringToBytes(string memory input) pure public returns(bytes memory res, uint len) {
        res = bytes(input);
        return (res, res.length);
    }
//-------------------------------------------ECC TOOL----------------------------------------------------------
    uint internal constant q = 0x30644e72e131a029b85045b68181585d2833e84879b9709143e1f593f0000001;
    
    //base point
    uint internal constant P_x = 1;
    uint internal constant P_y = 2;

    //sk
//    uint public x = 21888242871839275222246405741257275088548364400416034343698204186575808495617;
    //pk
    uint public X_x = 12687829327116033766044967272984875459725580445434891913526639723021250991356;
    uint public X_y = 6661625941897695847711477907189447581177451535292954288577207759710518185770;

    //off-chain function
    function sign(string memory message, uint x) internal view returns(uint d_x, uint d_y, uint s){
        uint r = generateRandomNumber(0);
        (uint R_x, uint R_y) = _g1Mul(P_x, P_y, r);
        uint hr = uint256(keccak256(abi.encodePacked(R_x, R_y)));
        uint hm = uint256(keccak256(abi.encodePacked(X_x, X_y, bytes(message))));
        s = addmod(mulmod(r, hr, q), mulmod(x, hm, q), q);
        (d_x, d_y) = _g1Mul(R_x, R_y, hr);
    }


    //on-chain verify with pk
    function verify(string memory message, uint d_x, uint d_y, uint s) internal view returns(bool isPass) {
        (uint h_x, uint h_y) = _g1Mul(X_x, X_y, uint256(keccak256(abi.encodePacked(X_x, X_y, bytes(message)))));
        (uint lft_x, uint lft_y) = _g1Mul(P_x, P_y, s);
        (uint rgt_x, uint rgt_y) = _g1Add(d_x, d_y, h_x, h_y);
        if (lft_x == rgt_x && lft_y == rgt_y) {
            return true;
        }
        return false;
    }

    function verify(uint h_m, uint d_x, uint d_y, uint s) internal view returns(bool isPass) {
        (uint h_x, uint h_y) = _g1Mul(X_x, X_y, h_m);
        (uint lft_x, uint lft_y) = _g1Mul(P_x, P_y, s);
        (uint rgt_x, uint rgt_y) = _g1Add(d_x, d_y, h_x, h_y);
        if (lft_x == rgt_x && lft_y == rgt_y) {
            return true;
        }
        return false;
    }


    function generateRandomNumber(uint256 seed) internal view returns (uint256) {
        bytes32 blockHash = blockhash(block.number - 1);
        bytes32 randomHash = keccak256(abi.encodePacked(blockHash, seed));
        return uint256(randomHash);
    }

	function _g1Add(uint x1, uint y1, uint x2, uint y2) internal view returns (uint, uint) {
		uint[4] memory input;
		input[0] = x1;
		input[1] = y1;
		input[2] = x2;
		input[3] = y2;

        uint256[2] memory r;
        assembly {
            if iszero(staticcall(not(0), 0x06, input, 0x80, r, 0x40)) {
                revert(0, 0)
            }
        }
        return (r[0], r[1]);
	}

	function _g1Mul(uint x1, uint y1, uint s) internal view returns (uint, uint) {
		uint[3] memory input;
		input[0] = x1;
		input[1] = y1;
		input[2] = s;

        uint256[2] memory r;
        assembly {
            if iszero(staticcall(not(0), 7, input, 0x80, r, 0x60)) {
                revert(0, 0)
            }
        }
        return (r[0], r[1]);
	}

}
